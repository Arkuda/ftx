@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kiryantsev.ftx.ftxcore.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/*
algo:
 * server await connection and info about max size of pool
 * looks to own pool, and choose lesser value
 * told client info about chosen pool size --- maybe not
 * clients send meta info about sending scope (files/folders/size)
 * server check is have need free space
 * if ok - open new connections for chosen pool sizes and told adresses for client
 * await data
 * if it multi-file sending -  (1 coroutine = 1 file )
 * else is single file - split file and send
*/



public class Client(
    private val ip: String,
    private val port: Int = 8099,

    ) {

//    public val messagesFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow<SocketMessage>()
//    public val progress: MutableSharedFlow<String> = MutableSharedFlow<String>()


    private val clientCoordinator = BaseSocketClient(
        onCreateClients = this::createClients
    )
    private val clientsPool = mutableListOf<BaseSocketClient>()

    private var poolCoordinator: PoolCoordinator? = null

    @OptIn(DelicateCoroutinesApi::class)
    public suspend fun init() {
        clientCoordinator.connect(ip = ip, port = port)
        clientCoordinator.coordinatePool()
        clientCoordinator.startHandleClientMessages()
        clientsPool.add(clientCoordinator)
        return suspendCoroutine { continuation ->
            GlobalScope.launch {
                withTimeout(timeout = 15.toDuration(DurationUnit.SECONDS)) {
                    clientCoordinator.state.filter { it == ClientState.READY }.collect {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }


    public fun sendFolder(path: String): Job {
        return GlobalScope.launch {
            return@launch withContext(Dispatchers.IO) {
                return@withContext suspendCoroutine { continuation ->
                    val filesToSend = FileTreeUtils.getFilesForDirectory(path).toMutableList()
                    poolCoordinator = PoolCoordinator(
                        pool = clientsPool,
                        files = filesToSend,
                        basePath = path,
                        onSendComplete = {
                            continuation.resume(Unit)
                        }

                    )
                }
            }
        }
    }


    private fun createClients(ports: List<Int>) {
        ports.forEach {
            val subClient = BaseSocketClient(onCreateClients = {})
            subClient.connect(ip = ip, port = it)
            subClient.startHandleClientMessages()
            clientsPool.add(subClient)
        }
    }
}


//
//
//@ExperimentalCoroutinesApi
//internal class BaseSocketClient(
//    private val onCreateClients: (List<Int>) -> Unit,
//    private val messagesFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow<SocketMessage>(),
//
//    ) {
//
//    private var client: Socket? = null
//    private val _state = MutableStateFlow(ClientState.NOT_CONNECTED)
////    private val coroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() )
////    private val coroutineScope = newSingleThreadContext(UUID.randomUUID().toString())
//
//    internal val state = _state.asStateFlow()
//
//    fun connect(ip: String, port: Int) {
//        _state.update { ClientState.CONNECTING }
//        client = Socket(ip, port)
//    }
//
//
//    fun coordinatePool() {
//        if (client?.isConnected == true) {
//            _state.update { ClientState.POOL_COORDINATION }
//            client!!.keepAlive = true
//            val availablePoolSize = 64
//
//            client!!.sendMessage(
//                AvailablePoolSizeMessage(
//                    size = availablePoolSize
//                )
//            )
//        }
//    }
//
//
//    // run in io dispatcher
//    fun startHandleClientMessages() {
////        if (client?.isConnected == true) {
////            GlobalScope.launch {
////                val scanner = Scanner(client!!.getInputStream())
////
////                while (ClientState.needWaitMessagesFromServer(_state.value)) {
////                    if (scanner.hasNextLine()) {
////                        try {
////                            val str = scanner.nextLine()
////                            val message = Json.decodeFromString<SocketMessage>(str)
////                            messagesFlow.emit(message)
////                            when (message) {
////                                is ChosenPoolSizeMessage -> {
////                                    Dispatchers.IO.limitedParallelism(
////                                        message.size * 2
////                                    )
////                                    onCreateClients(message.ports)
////                                    _state.update { ClientState.READY }
////                                }
////
////                                else -> {}
////                            }
////                        } catch (e: Exception) {
////                            println("Parse command from socket error: $e")
////                        }
////                    }
////                }
////            }
////        }
//    }
//
//
//    @Suppress("NewApi")
//    fun sendFile(file: File, basePath: String): Deferred<Boolean> =
////        coroutineScope.async {
////            val path = file.path.replace(basePath, "")
////            try {
////                _state.update { ClientState.DO_WORK }
////                val size = file.length()
////                val output = client?.getOutputStream()
////
////                client?.sendMessage(
////                    StartFileSendingMessage(
////                        sizeInBytes = size,
////                        relativePathWithName = path
////                    )
////                )
////
////                file.inputStream().transferTo(output!!)
////                output!!.apply {
////                    write(-1)
////                    flush()
////                }
////
////
////                return@async withTimeout(
////                    timeout = 15.toDuration(DurationUnit.SECONDS)
////                ) {
////                    suspendCoroutine<Boolean> { cont ->
////                        coroutineScope.launch {
////                            messagesFlow.filterIsInstance<FileReceivedMessage>().filter { it.path == path }
////                                .collect {
////                                    cont.resume(true)
////                                }
////                        }
////                        _state.update { ClientState.READY }
////                    }
////                }
////
////
////            } catch (e: Exception) {
////                println("Error of sending file $path $e")
////                _state.update { ClientState.READY }
////                return@async false
////            }
////        }
//
//
//    fun dispose() {
//        client?.close()
//    }
//
//
//
//    private fun Socket.sendMessage(socketMessage: SocketMessage) {
//        val writer = PrintWriter(getOutputStream())
//        writer.println(Json.encodeToString(socketMessage))
//        writer.flush()
//    }
//}


//
//internal class PoolCoordinator(
//    val pool: List<BaseSocketClient>,
//    val files: List<File>,
//    val basePath: String,
//    val onCompliteSending: () -> Unit,
//) {
//
//
//    private val filesToSend = files.toMutableList()
//
//    public val progress = MutableSharedFlow<String>()
//
//
//    @ExperimentalCoroutinesApi
//    fun start() = pool.forEach(this::takeNewFile)
//
//    private fun takeNewFile(client: BaseSocketClient) {
////        val file = filesToSend.firstOrNull()
////        var completeFilesCount = 0
////        if (file != null) {
////            //todo retrying and other stuff
////            filesToSend.remove(file)
////            val deferred = client.sendFile(file, basePath)
////
////            deferred.invokeOnCompletion {
////                if (it == null) {
////                    //take new file
////                    takeNewFile(client)
////                    completeFilesCount += 1
////                    // todo wtf - its block sending
//////                    GlobalScope.launch { progress.tryEmit("$completeFilesCount/${files.size}") }
////                } else {
////                    println("ClientUploadError : ${file.path} $it")
////                    // retry
////                    filesToSend.add(file)
////                    takeNewFile(client)
////                }
////            }
////        } else {
////           if(completeFilesCount >= files.size ){
////               onCompliteSending()
////           }
////        }
//    }
//
//
//}