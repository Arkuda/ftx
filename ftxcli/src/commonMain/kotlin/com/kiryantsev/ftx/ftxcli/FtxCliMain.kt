@file:OptIn(ExperimentalCli::class)

package com.kiryantsev.ftx.ftxcli

import com.kiryantsev.ftx.ftxcore.client.Client
import com.kiryantsev.ftx.ftxcore.server.Server
import kotlinx.cli.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.util.*
import javax.swing.JFrame


public fun main(args: Array<String>) {

    val parser = ArgParser("ftx")

    class ClientInstance : Subcommand("client", "Client instance - send files") {
        val serverAddr by parser.option(
            ArgType.String,
            shortName = "s",
            description = "Server address in ip format (client option)"
        )
        val sourceDirectory by parser.option(
            ArgType.String,
            shortName = "o",
            description = "Directory from which files will be transferred (client option)"
        )

        override fun execute() {
            if (serverAddr == null) {
                println("Server addr is required for client")
                kotlin.system.exitProcess(1)
            }
            if (sourceDirectory == null) {
                println("source Directory is required for client")
                kotlin.system.exitProcess(1)
            }
            println(">> Client started")

            val job = GlobalScope.async {
               return@async Client(serverAddr!!).sendFolder(sourceDirectory!!)
            }
            while (job.isActive) {

            }
        }

    }

    class ServerInstnce : Subcommand("server", "Server instance - receive files") {
        val directoryToSave by parser.option(
            ArgType.String,
            shortName = "p",
            description = "Directory for save files (server option)"
        )

        override fun execute() {
            if (directoryToSave == null) {
                println("directory to save is required for client")
                kotlin.system.exitProcess(1)
            }
            println(">> Server started")
            GlobalScope.launch {
                Server(directoryToSave!!).start()
            }
            infinityLoading()
        }


    }

    parser.subcommands(ClientInstance(), ServerInstnce())
    parser.parse(args)

}


private fun infinityLoading() {
//    val scanner = Scanner(System.`in`)
    while (true) {
//        scanner.nextLine()
    }
}