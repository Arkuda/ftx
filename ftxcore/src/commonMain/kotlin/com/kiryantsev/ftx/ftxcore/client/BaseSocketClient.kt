package com.kiryantsev.ftx.ftxcore.client

import com.kiryantsev.ftx.ftxcore.shared.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintWriter
import java.net.Socket
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration


internal class BaseSocketClient(
    private val onCreateClients: (List<Int>) -> Unit,
    private val messagesFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow<SocketMessage>(),
) {

    private var client: Socket? = null
    private var messageManager: SocketMessageManager? = null
    private val _state = MutableStateFlow(ClientState.NOT_CONNECTED)
    public val state = _state.asStateFlow()


    fun connect(ip: String, port: Int) {
        _state.update { ClientState.CONNECTING }
        client = Socket(ip, port)
        messageManager = SocketMessageManager(client!!)
    }


    fun coordinatePool() {
        if (client?.isConnected == true) {
            _state.update { ClientState.POOL_COORDINATION }
            client!!.keepAlive = true
            val availablePoolSize = 64

            messageManager!!.sendMessage(
                AvailablePoolSizeMessage(
                    size = availablePoolSize
                )
            )
        }
    }


    fun startHandleClientMessages() {
        if (client?.isConnected == true) {
            GlobalScope.launch {
                val scanner = Scanner(client!!.getInputStream())

                while (ClientState.needWaitMessagesFromServer(_state.value)) {
                    val message = messageManager?.receiveMessage() ?: continue
                    messagesFlow.emit(message)
                    when (message) {
                        is ChosenPoolSizeMessage -> {
                            Dispatchers.IO.limitedParallelism(
                                message.size * 2
                            )
                            onCreateClients(message.ports)
                            _state.update { ClientState.READY }
                        }
                        else -> {}
                    }

                }
            }
        }
    }


    fun sendFile(file: File, basePath: String, onFileSendComplete: (Exception?) -> Unit): Thread =
        Thread {
            val path = file.path.replace(basePath, "")
            try {
                _state.update { ClientState.DO_WORK }
                val size = file.length()
                val output = client?.getOutputStream()

                messageManager?.sendMessage(
                    StartFileSendingMessage(
                        sizeInBytes = size,
                        relativePathWithName = path
                    )
                )

                file.inputStream().transferTo(output!!)
                output!!.apply {
                    write(-1)
                    flush()
                }


                runBlocking {
                    val result = messageManager?.waitMessage({
                        when (it) {
                            is FileReceivedMessage -> it.path == path
                            is ErrorMessage -> true
                            else -> false
                        }
                    })

                    if (result != null) {
                        _state.update { ClientState.READY }

                        if (result is FileReceivedMessage) {
                            onFileSendComplete.invoke(null)
                        } else {
                            onFileSendComplete.invoke(java.lang.Exception("File send error"))
                        }
                    } else {
                        _state.update { ClientState.READY }
                        onFileSendComplete.invoke(java.lang.Exception("Error when send filed"))
                    }
                }

            } catch (e: Exception) {
                println("Error of sending file $path $e")
                _state.update { ClientState.READY }
                onFileSendComplete.invoke(e)
            }
        }.apply { start() }


}

internal enum class ClientState {
    NOT_CONNECTED,
    CONNECTING,
    POOL_COORDINATION,
    READY,
    DO_WORK,
    CLOSED;

    companion object {
        fun needWaitMessagesFromServer(state: ClientState) = state != NOT_CONNECTED && state != CLOSED
    }
}
