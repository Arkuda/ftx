package com.kiryantsev.ftx.ftxcore.client

import com.kiryantsev.ftx.ftxcore.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.DurationUnit
import kotlin.time.toDuration


internal class BaseSocketClient(
    private val onCreateClients: (List<Int>) -> Unit,
    private val messagesFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow<SocketMessage>(),
) {

    private var client: Socket? = null
    private val _state = MutableStateFlow(ClientState.NOT_CONNECTED)
    public val state = _state.asStateFlow()


    fun connect(ip: String, port: Int) {
        _state.update { ClientState.CONNECTING }
        client = Socket(ip, port)
    }


    fun coordinatePool() {
        if (client?.isConnected == true) {
            _state.update { ClientState.POOL_COORDINATION }
            client!!.keepAlive = true
            val availablePoolSize = 64

            client!!.sendMessage(
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
                    if (scanner.hasNextLine()) {
                        try {
                            val str = scanner.nextLine()
                            val message = Json.decodeFromString<SocketMessage>(str)
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
                        } catch (e: Exception) {
                            println("Parse command from socket error: $e")
                        }
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

                client?.sendMessage(
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
                    try {
                        withTimeout(
                            timeout = 15.toDuration(DurationUnit.SECONDS)
                        ) {
                            messagesFlow.filter {
                                when (it) {
                                    is FileReceivedMessage -> it.path == path
                                    is ErrorMessage -> true
                                    else -> false
                                }
                            }
                                .collect {
                                    _state.update { ClientState.READY }

                                    if (it is FileReceivedMessage){
                                        onFileSendComplete.invoke(null)
                                    }else {
                                        onFileSendComplete.invoke(java.lang.Exception("File send error"))
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        _state.update { ClientState.READY }
                        onFileSendComplete.invoke(e)
                    }
                }


            } catch (e: Exception) {
                println("Error of sending file $path $e")
                _state.update { ClientState.READY }
                onFileSendComplete.invoke(e)

            }
        }.apply { start() }

    private fun Socket.sendMessage(socketMessage: SocketMessage) {
        val writer = PrintWriter(getOutputStream())
        writer.println(Json.encodeToString(socketMessage))
        writer.flush()
    }

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
