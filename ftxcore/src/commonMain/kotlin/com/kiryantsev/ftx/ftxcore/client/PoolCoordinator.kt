package com.kiryantsev.ftx.ftxcore.client

import com.kiryantsev.ftx.ftxcore.shared.logging.LogManager
import com.kiryantsev.ftx.ftxcore.shared.logging.LogMessage
import kotlinx.coroutines.*
import okio.Path

internal class PoolCoordinator(
    val pool: List<BaseSocketClient>,
    val filesPaths: List<Path>,
    val basePath: String,
    val onSendComplete: () -> Unit,
) {

    private val coroutineContext = Dispatchers.IO + SupervisorJob()
//    private val coroutineContext = newSingleThreadContext("PoolCoordinator${this.hashCode()}")
    private val coroutineScope = CoroutineScope(coroutineContext)

    private val filesToSend = filesPaths.toMutableList()
    private var filesComplete = 0

    @Suppress("DEPRECATION")
    fun coordinate() {
        coroutineScope.launch {
            LogManager.log(LogMessage.StringLogMessage("CLIENT POOL COORDINATOR", "Start sendign ${filesToSend.size} files"))
            while (filesToSend.isNotEmpty()) {
                val idleClient = firstIdleSender()
                if (idleClient != null) {
                    filesToSend.firstOrNull()?.let { file ->
                        LogManager.log(LogMessage.StringLogMessage("CLIENT POOL COORDINATOR", "Files to send ${filesToSend.size}, choosed ${file.name}, sending"))

                            filesToSend.remove(file)
                            try {
                                idleClient.sendFile(
                                    filePath = file,
                                    basePath = basePath,
                                )
                                LogManager.log(LogMessage.StringLogMessage("Client","Progress $filesComplete/${filesPaths.size}"))
                                if(++filesComplete == filesPaths.size){
                                    onSendComplete()
                                } else { }
                            }catch (e: ClientException){
                                LogManager.log(
                                    LogMessage.ExceptionLogMessage(
                                        "Client",
                                        "Send file $file error, client exception $e",
                                        e
                                    )
                                )
                                filesToSend.add(file)
                            }catch (e: Exception){
                                LogManager.log(
                                    LogMessage.ExceptionLogMessage(
                                        "Client",
                                        "Send file $file error, exception $e",
                                        e
                                    )
                                )

                                filesToSend.add(file)
                            }
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }


    @Suppress("DEPRECATION")
    fun dispose() {
//        coroutineContext.close()
    }

    private fun firstIdleSender(): BaseSocketClient? =
        pool.firstOrNull { client -> client.state.value == ClientState.READY }


}