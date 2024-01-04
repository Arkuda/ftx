package com.kiryantsev.commonui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kiryantsev.commonui.screens.ClientOrServerScreen
import com.kiryantsev.commonui.screens.CurrentScreenEnum
import com.kiryantsev.commonui.screens.Navigator
import com.kiryantsev.commonui.screens.server.ServerScreen

@Composable
internal fun App() {

    val currentScreen = remember {  mutableStateOf(CurrentScreenEnum.CHOOSE_CLIENT_OR_SERVER) }

    val navigator = object : Navigator {
        override fun onChangeScreen(newScreen: CurrentScreenEnum) {
            currentScreen.value = newScreen
        }
    }


    when (currentScreen.value){
        CurrentScreenEnum.CHOOSE_CLIENT_OR_SERVER -> ClientOrServerScreen(navigator)
        CurrentScreenEnum.SERVER_SCREEN -> ServerScreen(navigator)
        CurrentScreenEnum.CLIENT_SCREEN -> TODO()
    }
}