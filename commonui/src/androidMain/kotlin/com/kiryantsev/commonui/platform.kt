package com.kiryantsev.commonui

import androidx.compose.runtime.Composable

public actual fun getPlatformName(): String {
    return "ftx"
}

@Composable
public fun UIShow() {
    App()
}