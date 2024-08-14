package com.kiryantsev.ftx.ftxcore.shared

import com.kiryantsev.ftx.ftxcore.shared.logging.LogManager
import com.kiryantsev.ftx.ftxcore.shared.logging.LogMessage
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SocketMessageManager(private val socket: Socket) {

    val receiveChannel: ByteReadChannel = socket.openReadChannel()
    val sendChannel: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)

    suspend fun sendMessage(msg: SocketMessage) {
        val jsonMsg = Json.encodeToString(msg)
        LogManager.log(LogMessage.StringLogMessage("MSG_MNGR", "Sending message: $jsonMsg"))
        sendChannel.writeStringUtf8("$jsonMsg\r")
    }

    suspend fun receiveMessage(): SocketMessage? {
        try {
            LogManager.log(LogMessage.StringLogMessage("MESSANGER ${socket.localAddress} ${socket.hashCode()}", "try receive, availableForRead=${receiveChannel.availableForRead}"))

//            if(receiveChannel.availableForRead == 0) return null
            val rawMessage = receiveChannel.readUTF8Line(1000) ?: return null
            val decodedMessage = Json.decodeFromString<SocketMessage>(rawMessage)
            return decodedMessage
        } catch (e: Exception) {
            LogManager.log(LogMessage.ExceptionLogMessage("SocketMessageManager", "Error while receive message $e",e))
            return null
        }
    }

    @Suppress("UNREACHABLE_CODE")
    /// WARNING - BLOCS COROUTINE, when timeout - return null
    suspend fun waitMessage(predicate: suspend (SocketMessage) -> Boolean, timeoutInSec: Int = 15): SocketMessage? {
        LogManager.log(LogMessage.StringLogMessage("MESSANGER ${socket.localAddress} ${socket.hashCode()}", " wait Message"))
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
        } catch (e: Exception) {
            return null
        }
    }




}