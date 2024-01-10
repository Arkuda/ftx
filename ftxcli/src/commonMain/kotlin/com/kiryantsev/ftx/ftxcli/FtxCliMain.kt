@file:OptIn(ExperimentalCli::class)

package com.kiryantsev.ftx.ftxcli

import com.kiryantsev.ftx.ftxcore.client.Client
import com.kiryantsev.ftx.ftxcore.server.Server
import kotlinx.cli.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


public fun main(args: Array<String>) {

    val parser = ArgParser("ftx")

    class ClientInstance : Subcommand("client", "Client instance - send files") {
        val serverAddr by parser.option(
            ArgType.String,
            shortName = "s",
            description = "Server address in ip format (client option)"
        ).required()
        val sourceDirectory by parser.option(
            ArgType.String,
            shortName = "i",
            description = "Directory from which files will be transferred (client option)"
        ).required()

        override fun execute() {
            GlobalScope.launch {Client(serverAddr).sendFolder(sourceDirectory) }
            while (true) {
            }

        }

    }

    class ServerInstnce : Subcommand("server", "Server instance - receive files") {
        val directoryToSave by parser.option(
            ArgType.String,
            shortName = "o",
            description = "Directory for save files (server option)"
        ).required()

        override fun execute() {
            GlobalScope.launch {
                Server(directoryToSave).start()
            }
            while (true) {
            }
        }


    }

    parser.subcommands(ClientInstance(), ServerInstnce())
    parser.parse(args)

}
