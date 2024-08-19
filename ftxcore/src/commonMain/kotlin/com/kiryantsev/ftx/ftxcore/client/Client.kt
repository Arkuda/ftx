@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.client

import com.kiryantsev.ftx.ftxcore.shared.logging.LogManager
import com.kiryantsev.ftx.ftxcore.shared.logging.LogMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/*
algo:
 * server await connection and info about max size of pool
 * looks to own pool, and choose lesser value
 * told client info about chosen pool size --- maybe not
 * clients send meta info about sending scope (files/folders/size)
 * server check is have need free space
 * if ok - open new connections for chosen pool sizes and told adresses for client
 * await data
 * if it multi-file sending -  (1 coroutine = 1 file )
 * else is single file - split file and send
*/



public class Client(
    private val ip: String,
    private val port: Int = 8099,

    ) {

    public val state : Flow<ClientState>
        get() = clientCoordinator.state

    private val clientCoordinator = BaseSocketClient(
        onCreateClients = this::createClients
    )
    private val clientsPool = mutableListOf<BaseSocketClient>()

    private var poolCoordinator: PoolCoordinator? = null

    private val coroutineContext = Dispatchers.IO + SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineContext)

    @OptIn(DelicateCoroutinesApi::class)
    public suspend fun init(): Boolean {
        clientCoordinator.connect(ip = ip, port = port)
        clientCoordinator.startHandleClientMessages()
        clientCoordinator.coordinatePool()
        clientsPool.add(clientCoordinator)
        return suspendCoroutine<Boolean> { continuation ->
            GlobalScope.launch {
                try {
                    withTimeout(timeout = 60.toDuration(DurationUnit.SECONDS)) {
                        clientCoordinator.state.first { it == ClientState.READY }
                        continuation.resume(true)
                    }
                } catch (e: Exception) {
                    LogManager.log(LogMessage.ExceptionLogMessage("Client", "Exception while try init client", e))
                    continuation.resumeWithException(e)
                }
            }
        }
    }


    public fun sendFolder(path: String, onCompleteCallback: (Exception?) -> Unit) {
        coroutineScope.launch {
            LogManager.log(LogMessage.StringLogMessage("Client", "Starting sending folder $path"))
            withContext(Dispatchers.IO) {
                try {
                    val filesToSend = FileTreeUtils.getFilesForDirectory(path).toMutableList()
                    LogManager.log(LogMessage.StringLogMessage("Client", "Finded ${filesToSend.size} files to send"))
                    poolCoordinator = PoolCoordinator(
                        pool = clientsPool,
                        filesPaths = filesToSend,
                        basePath = path,
                        onSendComplete = { onCompleteCallback(null) }
                    )
                    poolCoordinator!!.coordinate()
                } catch (e: Exception) {
                    LogManager.log(LogMessage.ExceptionLogMessage("Client", "Exception while sending folder", e))
                    onCompleteCallback(e)
                }
            }
        }
    }


    private suspend fun createClients(ports: List<Int>) {
       return ports.map {
            coroutineScope.launch {
                LogManager.log(LogMessage.StringLogMessage("Client", "Creating BaseSocketClient with port ${it}"))
                val subClient = BaseSocketClient(onCreateClients = {})
                subClient.connect(ip = ip, port = it)
                subClient.startHandleClientMessages()
                clientsPool.add(subClient)
                LogManager.log(LogMessage.StringLogMessage("Client", "Creating BaseSocketClient with port ${it} done"))
            }
        }.joinAll()
    }
}

