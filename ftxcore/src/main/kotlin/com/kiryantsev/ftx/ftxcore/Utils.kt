package com.kiryantsev.ftx.ftxcore

import java.io.File

class Utils {
    companion object {
        fun createDirs(path: String){
            File(path).apply {
                if (!this.exists()){
                    this.mkdirs()
                }
            }
        }
    }
}