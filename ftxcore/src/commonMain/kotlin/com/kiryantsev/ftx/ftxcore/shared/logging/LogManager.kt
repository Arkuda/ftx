package com.kiryantsev.ftx.ftxcore.shared.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public object LogManager {

    public var loggingEnabled: Boolean = false

    private val logMutFlow = MutableStateFlow<LogMessage>(LogMessage.StringLogMessage("Logger","Logger started"))
    public val logFlow: StateFlow<LogMessage> = logMutFlow.asStateFlow()

    public suspend fun log(msg: LogMessage){
        if(loggingEnabled){
            logMutFlow.emit(msg)
        }
    }

}