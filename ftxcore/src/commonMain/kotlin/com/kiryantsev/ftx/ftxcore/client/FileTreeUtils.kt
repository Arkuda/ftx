package com.kiryantsev.ftx.ftxcore.client

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal class FileTreeUtils {
    companion object {
        fun getFilesForDirectory(path: String): List<Path> {
            val fileList = mutableListOf<Path>()
            val foldersToScan = mutableListOf<Path>(path.toPath(true))
            val rootMetadata = FileSystem.SYSTEM.metadata(path.toPath(true))

            if(rootMetadata.isRegularFile){
                return foldersToScan
            }

            while (foldersToScan.isNotEmpty()) {
                val newFoldersToScan = mutableListOf<Path>()
                foldersToScan.forEach {
                    FileSystem.SYSTEM.listOrNull(it)?.forEach { item ->
                        val itemMetadata = FileSystem.SYSTEM.metadata(item)
                        if(itemMetadata.isRegularFile){
                            newFoldersToScan.add(item)
                        }
                        if(itemMetadata.isDirectory){
                            newFoldersToScan.add(item)
                        }
                    }
                }
                foldersToScan.clear()
                foldersToScan.addAll(newFoldersToScan)
            }

            return fileList
        }

    }
}