@file:OptIn(DelicateCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.server

import com.kiryantsev.ftx.ftxcore.Utils
import com.kiryantsev.ftx.ftxcore.shared.*
import com.kiryantsev.ftx.ftxcore.shared.logging.LogManager
import com.kiryantsev.ftx.ftxcore.shared.logging.LogMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use


//https://gist.github.com/Silverbaq/a14fe6b3ec57703e8cc1a63b59605876

@ExperimentalCoroutinesApi
internal class BaseSocketServer(
    private val port: Int,
    private val basePath: String,
    private val onCreateServersWithPorts: (Int) -> List<Int>,
) {

    var state: ServerState = ServerState.WAIT_CONNECTION
    val selectorManager = SelectorManager(Dispatchers.IO)
    private val socket = aSocket(selectorManager).tcp().bind(port = port)
    private lateinit var messageManager: SocketMessageManager

    private val coroutineContext = Dispatchers.IO + SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineContext)

    fun start() {
        coroutineScope.launch {
            while (true) {
                val connection = socket.accept()
                state = ServerState.CONNECTED
                messageManager = SocketMessageManager(socket = connection)
                LogManager.log(LogMessage.StringLogMessage("Server", "have connected client ${socket.localAddress}"))

                while (state != ServerState.CLOSED) {
                    tryReceiveMessage(connection)
                }
            }
        }

    }

    private suspend fun tryReceiveMessage(connection: Socket) {
        try {
            val message = messageManager.receiveMessage()
            if (message != null) {
                LogManager.log(
                    LogMessage.StringLogMessage(
                        "Server",
                        "received message $message, connection.isClosed = ${connection.isClosed}"
                    )
                )
                onMessageReceived(message, connection, messageManager)
            }

        } catch (e: Exception) {
            LogManager.log(
                LogMessage.ExceptionLogMessage(
                    "Server",
                    "error while parsing or receiving message $e",
                    e
                )
            )
        }
    }

    fun getResultPort(): Int = (socket.localAddress as InetSocketAddress).port

    private suspend fun onMessageReceived(
        message: SocketMessage,
        connection: Socket,
        messageManager: SocketMessageManager
    ) {
        when (message) {

            is AvailablePoolSizeMessage -> {
                val thisPoolSize = 10

                val chosenPoolSize = minOf(message.size, thisPoolSize)
                val chosenPorts = onCreateServersWithPorts(chosenPoolSize)

                messageManager.sendMessage(
                    ChosenPoolSizeMessage(
                        chosenPoolSize,
                        ports = chosenPorts
                    )
                )
                state = ServerState.AWAIT_MESSAGE
            }


            is StartFileSendingMessage -> {
                if (state != ServerState.AWAIT_MESSAGE) {
                    println("Socket message error: received StartFileSendingMessage when sate is $state")
                }

                receiveFile(messageManager, message, messageManager)
            }


            is CheckFreeSpaceForTransferMessage -> {
                messageManager.sendMessage(OkMessage) //todo calc free space
//                val targetFolderFreeSpace = File(basePath).freeSpace
//                if (targetFolderFreeSpace > message.size) {
//                    messageManager.sendMessage(OkMessage)
//                } else {
//                    messageManager.sendMessage(ErrorMessage)
//                }
            }

            else -> {}


        }
    }

    private fun receiveFile(
        client: SocketMessageManager,
        startFileSendingMessage: StartFileSendingMessage,
        messageManager: SocketMessageManager
    ) {
        coroutineScope.launch {
            try {
                state = ServerState.AWAIT_FILE
                val resPath = "$basePath/${startFileSendingMessage.relativePathWithName}"
                Utils.createDirs(resPath)
                LogManager.log(LogMessage.StringLogMessage("Server", "Start receiveing file $resPath"))

                val buffSize = 4000000
                val buff = ByteArray(buffSize)
                var readedCount = 0

                FileSystem.SYSTEM.sink(resPath.toPath(true)).buffer().use { sink ->
                    while (true) {
                        client.receiveChannel.readFully(
                            buff,
                            0,
                            buffSize
                        )
                        readedCount += buff.size

                        if (buff.isEmpty() && readedCount < startFileSendingMessage.sizeInBytes) {
                            // not full transmission error
                            GlobalScope.launch {
                                LogManager.log(
                                    LogMessage.StringLogMessage(
                                        "Server",
                                        "not full transmission error for file $resPath"
                                    )
                                )
                            }
                            client.sendMessage(RetryFileSend)
                            state = ServerState.AWAIT_MESSAGE
                            break
                        }
                        if (buff.isEmpty() && readedCount >= startFileSendingMessage.sizeInBytes) {
                            // ok
                            GlobalScope.launch {
                                LogManager.log(
                                    LogMessage.StringLogMessage(
                                        "Server",
                                        "File received done for file $resPath"
                                    )
                                )
                            }
                            client.sendMessage(FileReceivedMessage(startFileSendingMessage.relativePathWithName))
                            state = ServerState.AWAIT_MESSAGE
                            break
                        }
                    }
                }


            } catch (e: Exception) {
                state = ServerState.AWAIT_MESSAGE
                GlobalScope.launch {
                    LogManager.log(
                        LogMessage.ExceptionLogMessage(
                            "Server",
                            "Error when receive file ${startFileSendingMessage.relativePathWithName} : $e",
                            e
                        )
                    )
                }
                messageManager.sendMessage(ErrorMessage)
            }
        }
    }

    fun dispose() {
//        coroutineContext.close()
    }


}


public enum class ServerState {
    WAIT_CONNECTION,
    CONNECTED,

    //    POOL_COORDINATION,
    AWAIT_MESSAGE,
    AWAIT_FILE,
    CLOSED;


    internal companion object {
        fun isNeedListeningMessage(state: ServerState) = state == CONNECTED || state == AWAIT_MESSAGE
    }
}
