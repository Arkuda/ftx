@file:OptIn(ExperimentalCli::class)

package com.kiryantsev.ftx.ftxcli

import com.kiryantsev.ftx.ftxcore.client.Client
import com.kiryantsev.ftx.ftxcore.server.Server
import kotlinx.cli.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume



//https://ajalt.github.io/clikt/ - new kotlin native parser

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
                throw Exception("Server addr is required for client")

            }
            if (sourceDirectory == null) {
                println("source Directory is required for client")
                throw Exception("source Directory is required for client")
            }
            println(">> Client started")

            val job = GlobalScope.async {
               return@async suspendCancellableCoroutine<Exception?> { cont ->
                   Client(serverAddr!!).sendFolder(sourceDirectory!!){
                       cont.resume(it)
                   }
               }
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
                throw Exception("directory to save is required for client")
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