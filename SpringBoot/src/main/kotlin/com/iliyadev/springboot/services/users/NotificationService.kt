package com.iliyadev.springboot.services.users

import com.iliyadev.springboot.models.users.Notification
import com.iliyadev.springboot.repositories.users.NotificationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class NotificationService {
    
    @Autowired
    private lateinit var repository: NotificationRepository
    
    // دریافت اعلان‌های کاربر
    fun getByUser(userId: Long, pageIndex: Int, pageSize: Int): Page<Notification> {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(pageIndex, pageSize))
    }
    
    // دریافت اعلان‌های خوانده نشده
    fun getUnreadByUser(userId: Long): List<Notification> {
        return repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
    }
    
    // تعداد اعلان‌های خوانده نشده
    fun countUnread(userId: Long): Long = repository.countByUserIdAndIsReadFalse(userId)
    
    // خوانده شده کردن اعلان
    fun markAsRead(id: Long): Notification? {
        val notification = repository.findById(id).orElse(null) ?: return null
        return repository.save(notification.copy(isRead = true))
    }
    
    // ایجاد اعلان
    fun create(notification: Notification): Notification = repository.save(notification)
    
    // حذف اعلان
    fun delete(id: Long): Boolean {
        return if (repository.existsById(id)) {
            repository.deleteById(id)
            true
        } else false
    }
}
