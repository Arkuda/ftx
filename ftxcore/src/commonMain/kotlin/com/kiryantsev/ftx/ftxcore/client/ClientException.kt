package com.kiryantsev.ftx.ftxcore.client

public open class ClientException(message: String) : Exception(message)

public class ErrorWithSendingFileException(message: String): ClientException(message)