@file:OptIn(DelicateCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.server

import com.kiryantsev.ftx.ftxcore.Utils
import com.kiryantsev.ftx.ftxcore.data.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*


//https://gist.github.com/Silverbaq/a14fe6b3ec57703e8cc1a63b59605876

@ExperimentalCoroutinesApi
class BaseSocketServer(
    private val port: Int,
    private val basePath: String,
    private val onCreateServersWithPorts: (List<Int>) -> Unit,
) {

    private var state: ServerState = ServerState.WAIT_CONNECTION
    private val socket = ServerSocket(port)


    fun setThisServerToTransfer(){
        state = ServerState.AWAIT_MESSAGE
    }


    fun start() {
        val someDefferedToDispose = GlobalScope.async {
            return@async withContext(Dispatchers.IO) {
                return@withContext socket.accept()
            }
        }
        someDefferedToDispose.start()
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

        val scanner = Scanner(withContext(Dispatchers.IO) {
            client.getInputStream()
        })

        withContext(Dispatchers.IO) {
            while (client.isConnected) {
                if (scanner.hasNextLine() && ServerState.isNeedListeningMessage(state)) {
                    try {
                        val str = scanner.nextLine()
                        onMessageReceived(Json.decodeFromString<SocketMessage>(str), client)
                    } catch (e: Exception) {
                        println("Parse command from socket error: $e")
                    }
                }
            }
        }

    }

    private fun onMessageReceived(message: SocketMessage, client: Socket) {
        when (message) {

            is AvailablePoolSize -> {
                val thisPoolSize = 64
                Dispatchers.IO.limitedParallelism(thisPoolSize)

                val chosenPoolSize = minOf(message.size, thisPoolSize)
                val chosenPorts = (80000..(80000 + chosenPoolSize)).toMutableList().apply {
                    this[0] = port
                }

                onCreateServersWithPorts(chosenPorts)

                client.getOutputStream().write(
                    ChoosedPoolSize(
                        chosenPoolSize,
                        ports = chosenPorts
                    ).toStreamedMessage()
                )
                state = ServerState.AWAIT_MESSAGE
            }


            is StartFileSendingMessage -> {
                if (state != ServerState.AWAIT_MESSAGE) {
                    println("Socket message error: received StartFileSendingMessage when sate is $state")
                }
                state = ServerState.AWAIT_FILE
                receiveFile(client, message)
            }


            is CheckFreeSpaceForTransfer  -> {
              val targetFolderFreeSpace =  File(basePath).freeSpace
                if(targetFolderFreeSpace > message.size){
                    client.getOutputStream().write(OkMessage.toStreamedMessage())
                }else {
                    client.getOutputStream().write(ErrorMessage.toStreamedMessage())
                }
            }


        }
    }

    private fun receiveFile(client: Socket, startFileSendingMessage: StartFileSendingMessage) {
        try {
            val resPath = "$basePath/${startFileSendingMessage.relativePathWithName}"
            Utils.createDirs(resPath)
            val file = File(resPath)
            file.createNewFile()
            client.getInputStream().transferTo(file.outputStream(), startFileSendingMessage.sizeInBytes)
            client.getOutputStream().write(OkMessage.toStreamedMessage())
            state = ServerState.AWAIT_MESSAGE
        } catch (e: Exception) {
            println("Error when receive file ${startFileSendingMessage.relativePathWithName} : $e")
            client.getOutputStream().write(RetryFileSend.toStreamedMessage())

        }
    }

}

private fun InputStream.transferTo(outputStream: FileOutputStream, sizeInBytes: Int, bufferSize: Int = 1024) {
    var transferred: Long = 0
    val buffer = ByteArray(bufferSize)
    var read: Int

    while (this.read(buffer, 0, bufferSize).also { read = it } >= 0 && transferred < sizeInBytes) {
        outputStream.write(buffer, 0, read)
        transferred += read.toLong()
    }
}

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