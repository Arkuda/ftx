package com.kiryantsev.ftx.ftxcore.shared.logging

public sealed class LogMessage {

    public data class StringLogMessage(val tag: String, val message: String) : LogMessage()

    public data class ExceptionLogMessage(val tag: String, val message: String, val exception: Exception) : LogMessage()

}

