@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.client

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

    private val clientCoordinator = BaseSocketClient(
        onCreateClients = this::createClients
    )
    private val clientsPool = mutableListOf<BaseSocketClient>()

    private var poolCoordinator: PoolCoordinator? = null

    @OptIn(DelicateCoroutinesApi::class)
    public suspend fun init() {
        clientCoordinator.connect(ip = ip, port = port)
        clientCoordinator.startHandleClientMessages()
        clientCoordinator.coordinatePool()
        clientsPool.add(clientCoordinator)
        return suspendCoroutine { continuation ->
            GlobalScope.launch {
               try{
                   withTimeout(timeout = 15.toDuration(DurationUnit.SECONDS)) {
                       clientCoordinator.state.filter { it == ClientState.READY }.collect {
                           continuation.resume(Unit)
                       }
                   }
               }catch (e: Exception){
                   continuation.resumeWithException(e)
               }
            }
        }
    }


    public fun sendFolder(path: String): Job {
        return GlobalScope.launch {
            return@launch withContext(Dispatchers.IO) {
                return@withContext suspendCoroutine { continuation ->
                    val filesToSend = FileTreeUtils.getFilesForDirectory(path).toMutableList()
                    poolCoordinator = PoolCoordinator(
                        pool = clientsPool,
                        files = filesToSend,
                        basePath = path,
                        onSendComplete = {
                            continuation.resume(Unit)
                        }

                    )
                    poolCoordinator!!.coordinate()
                }
            }
        }
    }


    private fun createClients(ports: List<Int>) {
        ports.forEach {
            val subClient = BaseSocketClient(onCreateClients = {})
            subClient.connect(ip = ip, port = it)
            subClient.startHandleClientMessages()
            clientsPool.add(subClient)
        }
    }
}

