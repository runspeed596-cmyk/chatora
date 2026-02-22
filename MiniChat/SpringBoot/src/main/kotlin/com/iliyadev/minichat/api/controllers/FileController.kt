package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.core.storage.StorageService
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/files")
class FileController(
    private val storageService: StorageService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(FileController::class.java)

    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestHeader(value = "X-Match-Id", required = false) matchId: String?
    ): ResponseEntity<Map<String, String>> {
        return try {
            val filename = storageService.store(file, matchId)
            logger.info("UPLOADED file: $filename for match: $matchId")
            ResponseEntity.ok(mapOf("url" to "/api/files/$filename", "filename" to filename))
        } catch (e: Exception) {
            logger.error("UPLOAD FAILED: ${e.message}")
            ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "Upload failed")))
        }
    }

    @PostMapping("/cleanup/{matchId}")
    fun cleanupMatchFiles(@PathVariable matchId: String): ResponseEntity<Map<String, String>> {
        logger.info("CLEANUP triggered for matchId: $matchId")
        storageService.cleanup(matchId)
        return ResponseEntity.ok(mapOf("message" to "Cleanup successful for match $matchId"))
    }

    @GetMapping("/{filename}")
    fun getFile(@PathVariable filename: String): ResponseEntity<Resource> {
        val file = storageService.load(filename)
        if (!file.exists()) {
            return ResponseEntity.notFound().build()
        }
        val resource = FileSystemResource(file)
        
        val contentType = when (file.extension.lowercase()) {
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG
            "png" -> MediaType.IMAGE_PNG
            "mp4" -> MediaType.parseMediaType("video/mp4")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }

        return ResponseEntity.ok()
            .contentType(contentType)
            .body(resource)
    }
}
