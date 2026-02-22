package com.iliyadev.springboot.services.users

import com.iliyadev.springboot.models.users.Resume
import com.iliyadev.springboot.repositories.users.ResumeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.UUID

@Service
class ResumeService {
    
    @Autowired
    private lateinit var repository: ResumeRepository
    
    @Value("\${upload.path:uploads/resumes}")
    private lateinit var uploadPath: String
    
    // آپلود رزومه
    fun upload(file: MultipartFile, userId: Long): Resume? {
        try {
            // ایجاد پوشه در صورت عدم وجود
            val uploadDir = Paths.get(uploadPath)
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir)
            }
            
            // ایجاد نام یکتا
            val originalFileName = file.originalFilename ?: "resume"
            val extension = originalFileName.substringAfterLast(".", "pdf")
            val fileName = "${UUID.randomUUID()}.$extension"
            val filePath = uploadDir.resolve(fileName)
            
            // ذخیره فایل
            Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
            
            // ذخیره اطلاعات در دیتابیس
            val resume = Resume(
                fileName = fileName,
                originalFileName = originalFileName,
                filePath = filePath.toString(),
                fileSize = file.size,
                mimeType = file.contentType ?: "application/pdf",
                uploadedAt = LocalDateTime.now()
            )
            
            return repository.save(resume)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    // دریافت رزومه‌های کاربر
    fun getByUser(userId: Long): List<Resume> = repository.findByUserId(userId)
    
    // دریافت آخرین رزومه کاربر
    fun getLatestByUser(userId: Long): Resume? = repository.findFirstByUserIdOrderByUploadedAtDesc(userId)
    
    // حذف رزومه
    fun delete(id: Long): Boolean {
        val resume = repository.findById(id).orElse(null) ?: return false
        
        try {
            // حذف فایل
            Files.deleteIfExists(Paths.get(resume.filePath))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        repository.deleteById(id)
        return true
    }
}
