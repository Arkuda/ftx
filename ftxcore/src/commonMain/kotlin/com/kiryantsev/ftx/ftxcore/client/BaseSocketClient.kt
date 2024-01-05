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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ExperimentalCoroutinesApi
internal class BaseSocketClient(
    private val onCreateClients: (List<Int>) -> Unit,
    private val messagesFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow<SocketMessage>(),

    ) {

    private var client: Socket? = null
    private val _state = MutableStateFlow(ClientState.NOT_CONNECTED)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    internal val state = _state.asStateFlow()

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


    // run in io dispatcher
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


    @Suppress("NewApi")
    fun sendFile(file: File, basePath: String): Deferred<Boolean> =
        coroutineScope.async {
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


                return@async withTimeout(
                    timeout = 15.toDuration(DurationUnit.SECONDS)
                ) {
                    suspendCoroutine<Boolean> { cont ->
                        coroutineScope.launch {
                            messagesFlow.filterIsInstance<FileReceivedMessage>().filter { it.path == path }
                                .collect {
                                    cont.resume(true)
                                }
                        }
                        _state.update { ClientState.READY }
                    }
                }


            } catch (e: Exception) {
                println("Error of sending file $path $e")
                _state.update { ClientState.READY }
                return@async false
            }
        }


    fun dispose() {
        client?.close()
    }



    private fun Socket.sendMessage(socketMessage: SocketMessage) {
        val writer = PrintWriter(getOutputStream())
        writer.println(Json.encodeToString(socketMessage))
//        writer.println( socketMessage.toStreamedMessage())
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