package com.kiryantsev.ftx.ftxcore.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


abstract class SocketMessage {
    fun toStreamedMessage() : ByteArray{
        return Json.encodeToString(this).toByteArray(Charsets.UTF_8)
    }

}

@Serializable
object OkMessage : SocketMessage()

@Serializable
object RetryFileSend : SocketMessage()

@Serializable
object ErrorMessage: SocketMessage()

@Serializable
data class AvailablePoolSize(
    val size: Int
) : SocketMessage()


@Serializable
data class CheckFreeSpaceForTransfer(
    val size: Long
): SocketMessage()

@Serializable
data class ChoosedPoolSize(
    val size: Int,
    val ports: List<Int>
) : SocketMessage()

@Serializable
data class StartFileSendingMessage(
    val sizeInBytes: Int,
    val relativePathWithName: String,
) : SocketMessage()