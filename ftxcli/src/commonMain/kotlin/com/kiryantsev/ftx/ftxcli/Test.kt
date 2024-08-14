package com.kiryantsev.ftx.ftxcli

import com.kiryantsev.ftx.ftxcore.client.Client
import com.kiryantsev.ftx.ftxcore.server.Server
import com.kiryantsev.ftx.ftxcore.shared.logging.LogManager
import com.kiryantsev.ftx.ftxcore.shared.logging.LogMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

public fun main(args: Array<String>) {


    val sorucePath = "E:\\books"
    val targetPath = "U:\\test_res"

    val client = Client("192.168.1.76")
    val server = Server(sorucePath)

    var isFinished = false
    LogManager.loggingEnabled = true

    GlobalScope.launch {
        LogManager.logFlow.onEach {
            when(it){
                is LogMessage.ExceptionLogMessage -> {
                    println("${it.tag} :   ${it.message}")
                    println("${it.tag} :   ${it.exception.stackTraceToString()}")
                }
                is LogMessage.StringLogMessage -> println("${it.tag} :   ${it.message}")
            }
        }.collect()
    }


    (CoroutineScope(newFixedThreadPoolContext(5,"server"))).launch {
        server.start()
    }
    (CoroutineScope(newFixedThreadPoolContext(5,"client"))).launch {
        delay(5000)
        client.init()
        client.sendFolder(targetPath){
            isFinished = true
            println("Finished sending, exception: $it")
        }
    }

    while (!isFinished){}

}