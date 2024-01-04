package com.kiryantsev.commonui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
public fun ClientOrServerScreen(
    navigator: Navigator
) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    navigator.onChangeScreen(CurrentScreenEnum.CLIENT_SCREEN)
                },
                content = { Text("I'm send files") }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    navigator.onChangeScreen(CurrentScreenEnum.SERVER_SCREEN)

                },
                content = { Text("I'm receive files") }
            )
        }
    }

}

