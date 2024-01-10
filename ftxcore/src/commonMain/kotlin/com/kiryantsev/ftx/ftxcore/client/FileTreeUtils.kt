package com.kiryantsev.ftx.ftxcore.client

import java.io.File

internal class FileTreeUtils {
    companion object {
        fun getFilesForDirectory(path: String) : List<File>{
            val fileList = mutableListOf<File>()

            val foldersToScan = mutableListOf<File>(File(path))

            while(foldersToScan.isNotEmpty()){
                val newFoldersToScan =  mutableListOf<File>()
                foldersToScan.forEach { folder ->
                    folder.listFiles()?.forEach { item ->
                        if(item.isDirectory){
                           newFoldersToScan.add(item)
                        }
                        if(item.isFile && item.length() > 0){
                            fileList.add(item)
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