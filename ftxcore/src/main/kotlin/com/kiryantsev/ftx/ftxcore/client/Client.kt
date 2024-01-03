package com.kiryantsev.ftx.ftxcore.client


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



class Client {
}