@file:OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.server

import com.kiryantsev.ftx.ftxcore.shared.SocketMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow


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

    private val coreServer = BaseSocketServer(
        port = 8099,
        basePath = basePath,
        onCreateServersWithPorts = this::createAdditionalServers,
    )

    public val state: Flow<ServerState>
        get() = coreServer.state

    private val servers = mutableListOf(coreServer)

    public fun start() {
        coreServer.start()
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
            servers.add(subServ)
            subServ.start()

        }
        return chosenPorts
    }


    public fun stopAll() {
        servers.forEach {
            it.dispose()
        }
    }


}