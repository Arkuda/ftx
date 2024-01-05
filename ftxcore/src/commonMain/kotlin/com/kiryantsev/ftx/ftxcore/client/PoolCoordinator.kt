@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.client

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

internal class PoolCoordinator(
    val pool: List<BaseSocketClient>,
    val files: List<File>,
    val basePath: String,
    val onCompliteSending: () -> Unit,
) {


    private val filesToSend = files.toMutableList()

    public val progress = MutableSharedFlow<String>()


    @ExperimentalCoroutinesApi
    fun start() = pool.forEach(this::takeNewFile)

    private fun takeNewFile(client: BaseSocketClient) {
        val file = filesToSend.firstOrNull()
        var completeFilesCount = 0
        if (file != null) {
            //todo retrying and other stuff
            filesToSend.remove(file)
            val deferred = client.sendFile(file, basePath)

            deferred.invokeOnCompletion {
                if (it == null) {
                    //take new file
                    takeNewFile(client)
                    completeFilesCount += 1
                    // todo wtf - its block sending
//                    GlobalScope.launch { progress.tryEmit("$completeFilesCount/${files.size}") }
                } else {
                    println("ClientUploadError : ${file.path} $it")
                    // retry
                    filesToSend.add(file)
                    takeNewFile(client)
                }
            }
        } else {
           if(completeFilesCount >= files.size ){
               onCompliteSending()
           }
        }
    }


}