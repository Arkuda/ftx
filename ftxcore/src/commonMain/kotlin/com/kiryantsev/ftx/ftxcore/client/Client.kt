@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.client

import com.kiryantsev.ftx.ftxcore.data.FileReceivedMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlin.coroutines.resume
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
    private val port: Int = 8099
) {


    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val coordinator = BaseSocketClient(
        onCreateClients = this::createClients
    )

    private val clientsPool = mutableListOf<BaseSocketClient>()


    public suspend fun init() {
        coordinator.connect(ip = ip, port = port)
        coordinator.coordinatePool()
        coordinator.startHandleClientMessages()

        return withTimeout(
            timeout = 15.toDuration(DurationUnit.SECONDS)
        ) {
            suspendCoroutine<Boolean> { cont ->
                coroutineScope.launch {
                    coordinator.state.filter { it == ClientState.READY }.collect {
                        cont.resume(true)
                    }
                }
            }
        }
    }


    public suspend fun sendFolder(path: String) {
        return suspendCoroutine { continuation ->
            val filesToSend = FileTreeUtils.getFilesForDirectory(path).toMutableList()
            PoolCoordinator(
                pool = clientsPool,
                files = filesToSend,
                basePath = path,
                onCompliteSending = {
                    continuation.resume(Unit)
                }
            ).start()
        }
    }


    private fun createClients(ports: List<Int>) {
        ports.forEach {
            val subClient = BaseSocketClient {}
            subClient.connect(ip = ip, port = it)
            subClient.startHandleClientMessages()
            clientsPool.add(subClient)
        }
    }
}