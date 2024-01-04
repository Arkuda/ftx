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
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    navigator.onChangeScreen(CurrentScreenEnum.CLIENT_SCREEN)
                },
                content = { Text("I'm send files") }
            )
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = {
                    navigator.onChangeScreen(CurrentScreenEnum.SERVER_SCREEN)

                },
                content = { Text("I'm receive files") }
            )
        }
    }

}

