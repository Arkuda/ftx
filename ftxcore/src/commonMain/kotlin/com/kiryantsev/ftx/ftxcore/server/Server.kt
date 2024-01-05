@file:OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.server

import com.kiryantsev.ftx.ftxcore.data.SocketMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.Executors


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


public class Server(private val basePath: String) {

    public val messagesFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow<SocketMessage>()
    private val coroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() )


    private val coreServer = BaseSocketServer(
        port = 8099,
        basePath = basePath,
        onCreateServersWithPorts = this::createAdditionalServers,
        messagesFlow = messagesFlow,
    )

    private var disposableJobs: MutableList<Job> = mutableListOf()


    public fun start() {
        val coreJob = coroutineScope.launch {
            withContext(Dispatchers.IO) {
                coreServer.handleClientMessages(
                    client = coreServer.awaitClientConnection(),
                    coroutineScope = coroutineScope,
                )
            }
        }
        disposableJobs.add(coreJob)
        coreJob.start()
    }


    public fun stopAll() {
        disposableJobs.forEach(Job::cancel)
    }

    private fun createAdditionalServers(poolSize: Int): List<Int> {
        val chosenPorts = mutableListOf<Int>()

        repeat(poolSize) {
            val subServ = BaseSocketServer(
                port = 0,
                basePath = basePath,
                onCreateServersWithPorts = { _ -> listOf() }
            )
            chosenPorts.add(subServ.getResultPort())

            val subJob = coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    subServ.handleClientMessages(
                        client = subServ.awaitClientConnection(),
                        coroutineScope = coroutineScope
                    )
                    subServ.setThisServerToTransfer()
                }
            }
            disposableJobs.add(subJob)
        }
        return chosenPorts
    }


}