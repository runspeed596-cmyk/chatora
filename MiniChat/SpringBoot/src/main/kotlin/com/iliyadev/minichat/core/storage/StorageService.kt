package com.iliyadev.minichat.core.storage

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Service
class StorageService {
    private val root = Paths.get("uploads")

    private val matchFiles = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()

    init {
        try {
            Files.createDirectories(root)
        } catch (e: Exception) {
            throw RuntimeException("Could not initialize storage", e)
        }
    }

    fun store(file: MultipartFile, matchId: String? = null): String {
        val extension = file.originalFilename?.substringAfterLast(".", "bin") ?: "bin"
        val filename = "${UUID.randomUUID()}.$extension"
        Files.copy(file.inputStream, root.resolve(filename))
        
        if (matchId != null) {
            matchFiles.computeIfAbsent(matchId) { java.util.concurrent.ConcurrentHashMap.newKeySet() }.add(filename)
        }
        
        return filename
    }

    fun load(filename: String): File {
        return root.resolve(filename).toFile()
    }

    fun delete(filename: String) {
        Files.deleteIfExists(root.resolve(filename))
    }

    fun cleanup(matchId: String) {
        val filenames = matchFiles.remove(matchId) ?: return
        filenames.forEach { delete(it) }
    }
}
