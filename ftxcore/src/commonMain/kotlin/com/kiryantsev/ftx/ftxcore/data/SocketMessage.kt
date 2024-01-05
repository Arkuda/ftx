package com.kiryantsev.ftx.ftxcore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
public sealed class SocketMessage {
    public fun toJson() : String =Json.encodeToString(this)

}

@Serializable
@SerialName("OkMessage")
public object OkMessage : SocketMessage()

@Serializable
@SerialName("RetryFileSend")
public object RetryFileSend : SocketMessage()

@Serializable
@SerialName("ErrorMessage")
public object ErrorMessage: SocketMessage()

@Serializable
@SerialName("AvailablePoolSizeMessage")
public data class AvailablePoolSizeMessage(
    val size: Int
) : SocketMessage()

@Serializable
@SerialName("CheckFreeSpaceForTransferMessage")
public data class CheckFreeSpaceForTransferMessage(
    val size: Long,
): SocketMessage()

@Serializable
@SerialName("ChosenPoolSizeMessage")
public data class ChosenPoolSizeMessage(
    val size: Int,
    val ports: List<Int>
) : SocketMessage()

@Serializable
@SerialName("StartFileSendingMessage")
public data class StartFileSendingMessage(
    val sizeInBytes: Long,
    val relativePathWithName: String,
) : SocketMessage()


@Serializable
@SerialName("FileReceivedMessage")
public data class FileReceivedMessage(
    val path : String
): SocketMessage()
