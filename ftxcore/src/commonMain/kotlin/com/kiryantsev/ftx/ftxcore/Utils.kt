package com.kiryantsev.ftx.ftxcore

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM

public class Utils {
   public companion object {
        public fun createDirs(path: String): Unit = FileSystem.SYSTEM.createDirectories(
            path.toPath(normalize = true).parent!!
        )
    }
}


