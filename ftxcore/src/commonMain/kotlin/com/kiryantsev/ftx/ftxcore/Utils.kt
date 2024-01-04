package com.kiryantsev.ftx.ftxcore

import java.io.File

public class Utils {
   public companion object {
        public fun createDirs(path: String){
            File(path).apply {
                if (!this.exists()){
                    this.mkdirs()
                }
            }
        }
    }
}