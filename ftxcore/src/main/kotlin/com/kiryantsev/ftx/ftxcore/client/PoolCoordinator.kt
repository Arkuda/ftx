@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import kotlin.coroutines.suspendCoroutine

class PoolCoordinator(
    val pool: List<BaseSocketClient>,
    val files: List<File>,
    val basePath: String,
    val onCompliteSending: () -> Unit,
) {


    private val filesToSend = files.toMutableList()


    @ExperimentalCoroutinesApi
    fun start() = pool.forEach(this::takeNewFile)

    private fun takeNewFile(client: BaseSocketClient) {
        val file = filesToSend.firstOrNull()
        if (file != null) {
            //todo retrying and other stuff
            filesToSend.remove(file)
            val deferred = client.sendFile(file, basePath)
            deferred.invokeOnCompletion {
                if (it == null) {
                    //take new file
                    takeNewFile(client)
                } else {
                    println("ClientUploadError : ${file.path} $it")
                    // retry
                    filesToSend.add(file)
                    takeNewFile(client)
                }
            }
        } else {
            // file pool is empty
            val inWorkClients = pool.filter { it.state.value == ClientState.DO_WORK }.size
            if (inWorkClients == 0) {
                onCompliteSending.invoke()
            }
        }
    }


}