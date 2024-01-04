package com.kiryantsev.ftx.ftxcore.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


internal abstract class SocketMessage {
    fun toStreamedMessage() : ByteArray{
        return Json.encodeToString(this).toByteArray(Charsets.UTF_8)
    }

}

@Serializable
internal object OkMessage : SocketMessage()

internal object RetryFileSend : SocketMessage()

@Serializable
internal object ErrorMessage: SocketMessage()

@Serializable
internal data class AvailablePoolSizeMessage(
    val size: Int
) : SocketMessage()


@Serializable
internal data class CheckFreeSpaceForTransferMessage(
    val size: Long,
): SocketMessage()

@Serializable
internal data class ChosenPoolSizeMessage(
    val size: Int,
    val ports: List<Int>
) : SocketMessage()

@Serializable
internal data class StartFileSendingMessage(
    val sizeInBytes: Long,
    val relativePathWithName: String,
) : SocketMessage()


@Serializable
internal data class FileReceivedMessage(
    val path : String
): SocketMessage()
