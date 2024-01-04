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

    var client by remember { mutableStateOf<Client?>(null) }

    var chosenDirectory by remember { mutableStateOf<String?>(null) }
    var showChooseDirDialog by remember { mutableStateOf(false) }
    var ipAddresses by remember { mutableStateOf("") }

    var state by remember { mutableStateOf(ClientScreenState.ENTER_IP) }


    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {

        DirectoryPicker(showChooseDirDialog) { path ->
            showChooseDirDialog = false
            chosenDirectory = path
        }


        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val columnScope = this


            when (state) {
                ClientScreenState.ENTER_IP -> columnScope.apply {
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
                                } catch (e: Exception) {
                                    //todo show error
                                    snackbarHostState.showSnackbar("Error when connect to server $e")
                                    state = ClientScreenState.ENTER_IP
                                }
                            }
                        },
                        content = { Text("Connect") }
                    )
                }

                ClientScreenState.TRY_CONNECTING -> columnScope.apply {
                    Text("connecting")
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }

                ClientScreenState.CHOOSE_FOLDER -> columnScope.apply {
                    Text(
                        if (chosenDirectory == null)
                            "Choose folder to send"
                        else
                            "Chosen directory is $chosenDirectory"
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        content = {
                            Text("Choose folder")
                        },
                        onClick = {
                            showChooseDirDialog = true
                        },
                    )

                    if (chosenDirectory != null) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            content = { Text("Start sending") },
                            onClick = {
                                try {
                                    state = ClientScreenState.SENDING_FILES
                                    coroutineScope.launch {
                                        client?.sendFolder(chosenDirectory!!)?.invokeOnCompletion {
                                            state = ClientScreenState.DONE
                                        } ?: {
                                            state = ClientScreenState.ENTER_IP
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Have error while sending files\n" +
                                                            "Restart server app and try again"
                                                )
                                            }
                                        }

                                    }
                                } catch (e: Exception) {
                                    state = ClientScreenState.CHOOSE_FOLDER
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Error when start sending files $e")
                                    }
                                }

                            }
                        )
                    }
                }

                ClientScreenState.SENDING_FILES -> columnScope.apply {
                    Text("Sending files")
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }

                ClientScreenState.DONE -> columnScope.apply {
                    Text("Files sending complete, yay !")
                }
            }
        }


    }
}

private enum class ClientScreenState {
    ENTER_IP,
    TRY_CONNECTING,
    CHOOSE_FOLDER,
    SENDING_FILES,
    DONE
}