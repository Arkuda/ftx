@file:OptIn(DelicateCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.server

import com.kiryantsev.ftx.ftxcore.Utils
import com.kiryantsev.ftx.ftxcore.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.*


//https://gist.github.com/Silverbaq/a14fe6b3ec57703e8cc1a63b59605876

@ExperimentalCoroutinesApi
internal class BaseSocketServer(
    private val port: Int,
    private val basePath: String,
    private val onCreateServersWithPorts: (Int) -> List<Int>,
    private val messagesFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow<SocketMessage>(),
) {

    private var state: ServerState = ServerState.WAIT_CONNECTION
    private val socket = ServerSocket(port)


    fun setThisServerToTransfer() {
        state = ServerState.AWAIT_MESSAGE
    }

    fun getResultPort(): Int = socket.localPort

    fun start(coroutineScope: CoroutineScope) {
        val someDeferredToDispose = coroutineScope.async {
            return@async withContext(Dispatchers.IO) {
                return@withContext socket.accept()
            }
        }
        someDeferredToDispose.start()
    }

    suspend fun awaitClientConnection(): Socket {
        return withContext(Dispatchers.IO) {
            val connection = socket.accept()
            state = ServerState.POOL_COORDINATION
            return@withContext connection
        }
    }

    suspend fun handleClientMessages(client: Socket) {
        client.keepAlive = true
        GlobalScope.launch {
            while (client.isConnected) {
                val scanner = Scanner(client.getInputStream())

                if (scanner.hasNextLine() && ServerState.isNeedListeningMessage(state)) {
                    try {
                        val str = scanner.nextLine()
                        val message = Json.decodeFromString<SocketMessage>(str)
                        messagesFlow.emit(message)
                        onMessageReceived(message, client)
                    } catch (e: Exception) {
                        println("Parse command from socket error: $e")
                    }
                }
                delay(100)
            }
        }

    }

    private fun onMessageReceived(message: SocketMessage, client: Socket) {
        when (message) {

            is AvailablePoolSizeMessage -> {
                val thisPoolSize = 64
                Dispatchers.IO.limitedParallelism(thisPoolSize * 2)

                val chosenPoolSize = minOf(message.size, thisPoolSize)
                val chosenPorts = onCreateServersWithPorts(chosenPoolSize)

                PrintWriter(client.getOutputStream()).apply {
                    println(
                        ChosenPoolSizeMessage(
                            chosenPoolSize,
                            ports = chosenPorts
                        ).toJson()
                    )
                    flush()
                }
                state = ServerState.AWAIT_MESSAGE
            }


            is StartFileSendingMessage -> {
                if (state != ServerState.AWAIT_MESSAGE) {
                    println("Socket message error: received StartFileSendingMessage when sate is $state")
                }
                state = ServerState.AWAIT_FILE
                receiveFile(client, message)
            }


            is CheckFreeSpaceForTransferMessage -> {
                val targetFolderFreeSpace = File(basePath).freeSpace
                if (targetFolderFreeSpace > message.size) {
                    PrintWriter(client.getOutputStream()).apply {
                        println(
                            OkMessage.toJson()
                        )
                        flush()
                    }

                } else {
                    PrintWriter(client.getOutputStream()).apply {
                        println(
                            ErrorMessage.toJson()
                        )
                        flush()
                    }
                }
            }

            else -> {}


        }
    }

    private fun receiveFile(client: Socket, startFileSendingMessage: StartFileSendingMessage) {
        try {
            val resPath = "$basePath/${startFileSendingMessage.relativePathWithName}"
            Utils.createDirs(resPath)
            val file = File(resPath)
            file.createNewFile()

            client.getInputStream().transferTo(file.outputStream(), startFileSendingMessage.sizeInBytes)

            PrintWriter(client.getOutputStream()).apply {
                println(
                    FileReceivedMessage(startFileSendingMessage.relativePathWithName).toJson()
                )
                flush()
            }
            state = ServerState.AWAIT_MESSAGE
        } catch (e: Exception) {
            println("Error when receive file ${startFileSendingMessage.relativePathWithName} : $e")
            PrintWriter(client.getOutputStream()).apply {
                println(
                    ErrorMessage.toJson()
                )
                flush()
            }

        }
    }


}

private fun InputStream.transferTo(outputStream: FileOutputStream, sizeInBytes: Long, bufferSize: Int = 8192) {
    var transferred: Long = 0
    val buffer = ByteArray(bufferSize)
    var countOfLoadedBytesInThisCycleBytes = 1

    countOfLoadedBytesInThisCycleBytes = this.read(buffer)

    while ((countOfLoadedBytesInThisCycleBytes > 0) && transferred < sizeInBytes) {
        transferred += countOfLoadedBytesInThisCycleBytes
        outputStream.write(buffer, 0, countOfLoadedBytesInThisCycleBytes)
        countOfLoadedBytesInThisCycleBytes = this.read(buffer)
    }
    outputStream.write(buffer, 0, countOfLoadedBytesInThisCycleBytes)
    outputStream.flush()
}

//private fun InputStream.transferTo(outputStream: FileOutputStream, sizeInBytes: Long, bufferSize: Int = 1024) {
//    var transferred: Long = 0
//    val buffer = ByteArray(bufferSize)
//    var read: Int
//
//    while (this.read(buffer, 0, bufferSize).also { read = it } >= 0 && transferred < sizeInBytes) {
//        outputStream.write(buffer, 0, read)
//        transferred += read.toLong()
//    }
//}



private enum class ServerState {
    WAIT_CONNECTION,
    POOL_COORDINATION,
    AWAIT_MESSAGE,
    AWAIT_FILE,
    CLOSED;


    companion object {
        fun isNeedListeningMessage(state: ServerState) = state == POOL_COORDINATION || state == AWAIT_MESSAGE
    }
}