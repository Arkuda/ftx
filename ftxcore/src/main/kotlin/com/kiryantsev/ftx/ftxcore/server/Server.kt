@file:OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.server

import kotlinx.coroutines.*


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


class Server(val basePath: String) {

    private val coreServer = BaseSocketServer(
        port = 8099,
        basePath = basePath,
        onCreateServersWithPorts = this::createAdditionalServers
    )

    private var disposableJobs: MutableList<Job> = mutableListOf()


    fun start() {
        val coreJob = GlobalScope.launch {
            withContext(Dispatchers.IO) {
                coreServer.handleClientMessages(coreServer.awaitClientConnection())
            }
        }
        disposableJobs.add(coreJob)
        coreJob.start()
    }


    fun stopAll() {
        disposableJobs.forEach(Job::cancel)
    }

    private fun createAdditionalServers(ports: List<Int>) {
        ports.forEach { port ->
            val subJob = GlobalScope.launch {
                withContext(Dispatchers.IO){
                  val subServ = BaseSocketServer(
                        port = port ,
                        basePath = basePath,
                        onCreateServersWithPorts = {}
                    )
                    subServ.handleClientMessages(subServ.awaitClientConnection())
                    subServ.setThisServerToTransfer()
                }
            }
            disposableJobs.add(subJob)
        }
    }


}