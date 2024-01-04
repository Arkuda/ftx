package com.kiryantsev.commonui.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.kiryantsev.commonui.screens.Navigator
import com.kiryantsev.ftx.ftxcore.client.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
public fun ClientScreen(
    navigator: Navigator
) {
    Surface {
        var client by remember { mutableStateOf<Client?>(null) }
        var isClientConnected by remember { mutableStateOf(false) }

        var chosenDirectory by remember { mutableStateOf<String?>(null) }
        var showChooseDirDialog by remember { mutableStateOf(false) }
        var ipAddresses by remember { mutableStateOf("") }

        var isSending by remember { mutableStateOf(false) }

        var state by remember { mutableStateOf(ClientScreenState.ENTER_IP) }


        val coroutineScope = remember { CoroutineScope(Dispatchers.Unconfined) }



        DirectoryPicker(showChooseDirDialog) { path ->
            showChooseDirDialog = false
            chosenDirectory = path
        }


        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            when (state){
                ClientScreenState.ENTER_IP -> {
                    TextField(
                        value = ipAddresses,
                        onValueChange = { newStr -> ipAddresses = newStr }
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            client = Client(ipAddresses)
                            state = ClientScreenState.TRY_CONNECTING
                            coroutineScope.launch {
                                try {
                                    client!!.init()
                                }catch (e: Exception){
                                    //todo show error
                                    state = ClientScreenState.ENTER_IP
                                }
                            }
                        },
                        content = { Text("Connect") }
                    )
                }
                ClientScreenState.TRY_CONNECTING ->  {
                    CircularProgressIndicator()
                }
                ClientScreenState.CHOOSE_FOLDER -> TODO()
                ClientScreenState.SENDING_FILES -> TODO()
                ClientScreenState.DONE -> TODO()
            }

            if (!isClientConnected) {
                //ip

            } else {
                if (!isSending) {
                    //choose folder
                } else {
                    // show progress
                }
            }
        }


    }
}

private enum class ClientScreenState{
    ENTER_IP,
    TRY_CONNECTING,
    CHOOSE_FOLDER,
    SENDING_FILES,
    DONE
}