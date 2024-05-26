@file:OptIn(DelicateCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.server

import com.kiryantsev.ftx.ftxcore.Utils
import com.kiryantsev.ftx.ftxcore.shared.*
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

    @Volatile
    var state: ServerState = ServerState.WAIT_CONNECTION
    private val socket = ServerSocket(port)
    private lateinit var messageManager: SocketMessageManager

    fun start() {
        socket.accept()
        val connection = socket.accept()
        state = ServerState.CONNECTED
        connection.keepAlive = true
        messageManager = SocketMessageManager(socket = connection)

        while (connection.isConnected) {
            if (ServerState.isNeedListeningMessage(state)) {
                runBlocking {
                    val message = messageManager.receiveMessage()
                    if(message != null){
                        onMessageReceived(message, connection, messageManager)
                    }
                }

            }
        }
    }

    fun getResultPort(): Int = socket.localPort

    private fun onMessageReceived(message: SocketMessage, connection: Socket, messageManager: SocketMessageManager) {
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

                receiveFile(connection, message, messageManager)
            }


            is CheckFreeSpaceForTransferMessage -> {
                val targetFolderFreeSpace = File(basePath).freeSpace
                if (targetFolderFreeSpace > message.size) {
                    messageManager.sendMessage(OkMessage)
                } else {
                    messageManager.sendMessage(ErrorMessage)
                }
            }

            else -> {}


        }
    }

    private fun receiveFile(client: Socket, startFileSendingMessage: StartFileSendingMessage, messageManager: SocketMessageManager) {
        try {
            state = ServerState.AWAIT_FILE
            val resPath = "$basePath/${startFileSendingMessage.relativePathWithName}"
            Utils.createDirs(resPath)
            val file = File(resPath)
            file.createNewFile()

            client.getInputStream().transferTo(file.outputStream(), startFileSendingMessage.sizeInBytes)


            state = ServerState.AWAIT_MESSAGE

            messageManager.sendMessage(FileReceivedMessage(startFileSendingMessage.relativePathWithName))


        } catch (e: Exception) {
            state = ServerState.AWAIT_MESSAGE
            println("Error when receive file ${startFileSendingMessage.relativePathWithName} : $e")
            messageManager.sendMessage(ErrorMessage)
        }
    }


}

private fun PrintWriter.sendMessage(message: SocketMessage) {
    println(message.toJson())
    flush()
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
    outputStream.close()
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