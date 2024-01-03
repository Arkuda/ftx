package com.kiryantsev.ftx.ftxcore.client

import com.kiryantsev.ftx.ftxcore.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.io.File
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ExperimentalCoroutinesApi
class BaseSocketClient(
    private val onCreateClients: (List<Int>) -> Unit,
) {

    private var client: Socket? = null
    private val _state = MutableStateFlow(ClientState.NOT_CONNECTED)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val messagesFlow = MutableSharedFlow<SocketMessage>()

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

            client!!.getOutputStream().write(
                AvailablePoolSizeMessage(
                    size = availablePoolSize
                ).toStreamedMessage()
            )
        }
    }


    // run in io dispatcher
    fun startHandleClientMessages() {
        if (client?.isConnected == true) {
            coroutineScope.launch {
                while (ClientState.needWaitMessagesFromServer(_state.value)) {
                    val messageJson = client!!.getInputStream().reader().readText()
                    val message = Json.decodeFromString<SocketMessage>(messageJson)
                    messagesFlow.emit(message)
                    when (message) {
                        is ChosenPoolSizeMessage -> {
                            Dispatchers.IO.limitedParallelism(
                                message.size
                            )
                            onCreateClients(message.ports)
                            _state.update { ClientState.READY }
                        }
                    }


                }
            }
        }
    }


    suspend fun sendFile(file: File, basePath: String): Boolean =  withContext(coroutineScope.coroutineContext) {
        val path = file.path.replace(basePath, "")
        try {
            _state.update { ClientState.DO_WORK }
            val size = file.length()
            val output = client?.getOutputStream()
            output?.write(
                StartFileSendingMessage(
                    sizeInBytes = size,
                    relativePathWithName = path
                ).toStreamedMessage()
            )

            file.inputStream().transferTo(output!!)

            return@withContext withTimeout(
                timeout = 15.toDuration(DurationUnit.SECONDS)
            ) {
                suspendCoroutine<Boolean> { cont ->
                    coroutineScope.launch {
                        messagesFlow.filterIsInstance<FileReceivedMessage>().filter { it.path == path }
                            .collectLatest {
                                cont.resume(true)
                            }
                    }
                    _state.update { ClientState.READY }
                }
            }


        } catch (e: Exception) {
            println("Error of sending file $path $e")
            _state.update { ClientState.READY }
            return@withContext false
        }
    }


    fun dispose() {
        client?.close()
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