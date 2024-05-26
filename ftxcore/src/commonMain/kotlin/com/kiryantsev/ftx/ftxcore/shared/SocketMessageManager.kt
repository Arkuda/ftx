package com.kiryantsev.ftx.ftxcore.shared

import com.kiryantsev.ftx.ftxcore.server.ServerState
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter
import java.net.Socket
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SocketMessageManager(socket: Socket) {

    private val scanner = Scanner(socket.getInputStream())
    private val printer = PrintWriter(socket.getOutputStream())

    fun sendMessage(msg: SocketMessage) {
        printer.println(Json.encodeToString(msg))
        printer.flush()
    }

    suspend fun receiveMessage(): SocketMessage? {
        return suspendCoroutine { cont ->
            if (scanner.hasNextLine()) {
                try {
                    val str = scanner.nextLine()
                    val message = Json.decodeFromString<SocketMessage>(str)
                    cont.resume(message)
                } catch (e: Exception) {
                    println("Parse command from socket error: $e")
                    cont.resumeWithException(e)
                }
            }else {
                cont.resume(null)
            }
        }
    }

    @Suppress("UNREACHABLE_CODE")
    /// WARNING - BLOCS COROUTINE, when timeout - return null
    suspend fun waitMessage(predicate: suspend (SocketMessage) -> Boolean, timeoutInSec : Int = 15): SocketMessage? {
        try {
            return withTimeout(timeout = timeoutInSec.toDuration(DurationUnit.SECONDS)) {
                while (true) {
                    val thisMessage = receiveMessage() ?: continue
                    if (predicate(thisMessage)) {
                        return@withTimeout thisMessage
                    }
                }
                return@withTimeout ErrorMessage
            }
        }catch (e: Exception){
            return null
        }
    }


}