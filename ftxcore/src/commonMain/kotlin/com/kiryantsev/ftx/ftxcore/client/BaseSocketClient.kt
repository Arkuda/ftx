package com.kiryantsev.ftx.ftxcore.client

import com.kiryantsev.ftx.ftxcore.shared.*
import com.kiryantsev.ftx.ftxcore.shared.SocketMessageManager
import com.kiryantsev.ftx.ftxcore.shared.logging.LogManager
import com.kiryantsev.ftx.ftxcore.shared.logging.LogMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import kotlin.coroutines.resume

internal class BaseSocketClient(
    private val onCreateClients: suspend (List<Int>) -> Unit,
) {

    private val _state = MutableStateFlow(ClientState.NOT_CONNECTED)
    val state = _state.asStateFlow()

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private var socket: Socket? = null
    private lateinit var socketMessageManager: SocketMessageManager

    private val coroutineContext = Dispatchers.IO + SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineContext)


    suspend fun connect(ip: String, port: Int) {
        try {
            _state.update { ClientState.CONNECTING }
            socket = aSocket(selectorManager).tcp().connect(ip, port)
            socketMessageManager = SocketMessageManager(socket!!, SocketMessageManager.SenderType.CLIENT)
            _state.update { ClientState.READY }
        } catch (e: Exception) {
            _state.update { ClientState.NOT_CONNECTED }
            throw e
        }
    }

    suspend fun coordinatePool() {
        return suspendCancellableCoroutine { cont ->
            coroutineScope.launch {
                LogManager.log(LogMessage.StringLogMessage("Client", "Starting coordination pool"))
                _state.update { ClientState.POOL_COORDINATION }
                socketMessageManager.sendMessage(AvailablePoolSizeMessage(10)) //todo change to calc value
                val choosedMessage =
                    socketMessageManager.waitMessage(
                        predicate = { it is ChosenPoolSizeMessage },
                        timeoutInSec = 60
                    ) as ChosenPoolSizeMessage?
                        ?: throw Exception("Pool coordination error")

                LogManager.log(
                    LogMessage.StringLogMessage(
                        "Client",
                        "Creating server pool for ports : ${choosedMessage.ports.joinToString()}"
                    )
                )
                onCreateClients(choosedMessage.ports)
                cont.resume(Unit)
            }
        }

    }


    fun startHandleClientMessages() {
//        coroutineScope.launch {
//
//
//        }
    }


    suspend fun sendFile(filePath: Path, basePath: String) {
        coroutineScope.launch {
            _state.update { ClientState.DO_WORK }
            LogManager.log(
                LogMessage.StringLogMessage(
                    "Client",
                    "Start sending file = $filePath"
                )
            )
            val size =
                FileSystem.SYSTEM.metadata(filePath).size
                    ?: throw ErrorWithSendingFileException("Cant calculate file size")

            socketMessageManager.sendMessage(StartFileSendingMessage(size, basePath))
            FileSystem.SYSTEM.read(filePath) {
                while (true) {
                    val bytes = readByteArray(4000000)
                    socketMessageManager.sendChannel.writeFully(
                        bytes,
                        0,
                        bytes.size
                    )
                    if (bytes.isEmpty()) {
                        break
                    }
                }
            }
            val receivedMessage = socketMessageManager.waitMessage(
                predicate = { it is OkMessage || it is ErrorMessage || it is RetryFileSend }
            ) ?: throw ErrorWithSendingFileException("No response to sended file")

            if (receivedMessage is ErrorMessage || receivedMessage is RetryFileSend) {
                throw ErrorWithSendingFileException("Error with sending file, see server log")
            }
            _state.update { ClientState.READY }
        }

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
