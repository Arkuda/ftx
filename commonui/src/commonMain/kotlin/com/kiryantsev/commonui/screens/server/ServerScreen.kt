package com.kiryantsev.commonui.screens.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.kiryantsev.commonui.screens.Navigator
import com.kiryantsev.ftx.ftxcore.server.Server
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*


@Composable
public fun ServerScreen(
    navigator: Navigator
) {

    var chosenDirectory by remember { mutableStateOf<String?>(null) }
    var showChooseDirDialog by remember { mutableStateOf(false) }
    var isServerStarted by remember { mutableStateOf(false) }
    var server by remember { mutableStateOf<Server?>(null) }
    var ipAddresses by remember { mutableStateOf("") }
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

            if (!isServerStarted) {
                Text(
                    if (chosenDirectory == null)
                        "Need choose directory"
                    else
                        "Chosen directory $chosenDirectory"
                )


                Button(
                    content = { Text("Choose target directory") },
                    onClick = {
                        showChooseDirDialog = true
                    }
                )


                if (chosenDirectory != null) {
                    Button(
                        content = { Text("Next") },
                        onClick = {
                            isServerStarted = true
                            server = Server(basePath = chosenDirectory!!)
                            server?.start()
                            coroutineScope.launch {
                                server!!.messagesFlow.collect {
                                    snackbarHostState.showSnackbar(it.toString())
                                }
                            }
                        }
                    )
                }
            } else {
                LaunchedEffect(Unit) {
                    val addresses = mutableListOf<String>()
                    val e: Enumeration<*> = NetworkInterface.getNetworkInterfaces()
                    while (e.hasMoreElements()) {
                        val n = e.nextElement() as NetworkInterface
                        val ee: Enumeration<*> = n.inetAddresses
                        while (ee.hasMoreElements()) {
                            val i = ee.nextElement() as InetAddress
                            addresses.add(i.hostAddress)
                        }
                    }
                    ipAddresses = addresses.firstOrNull { it.startsWith("192.168") } ?: addresses.joinToString(
                        transform = { it },
                        separator = "\n"
                    )
                }

                Text("Waiting connection")
                Text("You address: $ipAddresses")
            }

        }


    }

}

