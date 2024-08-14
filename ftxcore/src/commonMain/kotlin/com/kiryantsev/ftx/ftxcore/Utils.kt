package com.kiryantsev.ftx.ftxcore

import okio.FileSystem
import okio.Path.Companion.toPath

public class Utils {
   public companion object {
        public fun createDirs(path: String): Unit = FileSystem.SYSTEM.createDirectories(path.toPath(normalize = true))
    }
}