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

object RetryFileSend : SocketMessage()

@Serializable
object ErrorMessage: SocketMessage()

@Serializable
data class AvailablePoolSizeMessage(
    val size: Int
) : SocketMessage()


@Serializable
data class CheckFreeSpaceForTransferMessage(
    val size: Long,
): SocketMessage()

@Serializable
data class ChosenPoolSizeMessage(
    val size: Int,
    val ports: List<Int>
) : SocketMessage()

@Serializable
data class StartFileSendingMessage(
    val sizeInBytes: Long,
    val relativePathWithName: String,
) : SocketMessage()


@Serializable
data class FileReceivedMessage(
    val path : String
): SocketMessage()
