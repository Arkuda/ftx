package com.kiryantsev.ftx.ftxcore.client

import kotlinx.coroutines.flow.asFlow
import java.io.File

internal class PoolCoordinator(
    val pool: List<BaseSocketClient>,
    val files: List<File>,
    val basePath: String,
    val onSendComplete: () -> Unit,
) {


    private var coordinatorThread: Thread? = null
    private val filesToSend = files.toMutableList()
    private var filesComplete = 0

    @Suppress("DEPRECATION")
    fun coordinate() {
        coordinatorThread = Thread {
            while (filesToSend.isNotEmpty()) {
                val idleClient = firstIdleSender()
                if (idleClient != null) {
                    filesToSend.firstOrNull()?.let { file ->

                        var sendThread: Thread? = null
                        sendThread = Thread {
                            filesToSend.remove(file)
                            idleClient.sendFile(
                                file = file,
                                basePath = basePath,
                                onFileSendComplete = { exception ->
                                    if (exception == null) {
                                        if(++filesComplete == files.size){
                                            onSendComplete()
                                        }
                                    } else {
                                        filesToSend.add(file)
                                    }
                                    sendThread?.stop()
                                }
                            )
                        }.apply { start() }
                    }
                } else {
                    Thread.sleep(1000)
                }
            }
        }.apply { start() }
    }


    @Suppress("DEPRECATION")
    fun dispose() {
        coordinatorThread?.stop()
    }

    private fun firstIdleSender(): BaseSocketClient? =
        pool.firstOrNull { client -> client.state.value == ClientState.READY }


}