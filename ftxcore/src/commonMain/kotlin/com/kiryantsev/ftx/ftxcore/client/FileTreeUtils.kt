package com.kiryantsev.ftx.ftxcore.client

import java.io.File

internal class FileTreeUtils {
    companion object {
        fun getFilesForDirectory(path: String) : List<File>{
            val fileList = mutableListOf<File>()

            val foldersToScan = mutableListOf<File>(File(path))

            while(foldersToScan.isNotEmpty()){
                foldersToScan.forEach { folder ->
                    folder.listFiles()?.forEach { item ->
                        if(item.isDirectory){
                            foldersToScan.add(item)
                        }
                        if(item.isFile){
                            fileList.add(item)
                        }
                    }
                    foldersToScan.remove(folder)
                }
            }

            return fileList
        }

    }
}