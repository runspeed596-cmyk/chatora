package com.iliyadev.springboot.services.users

import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.models.jobs.JobPosting
import com.iliyadev.springboot.models.users.Bookmark
import com.iliyadev.springboot.repositories.users.BookmarkRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookmarkService {
    
    @Autowired
    private lateinit var repository: BookmarkRepository
    
    // دریافت نشان‌شده‌های کاربر
    fun getByUser(userId: Long, pageIndex: Int, pageSize: Int): Page<Bookmark> {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(pageIndex, pageSize))
    }
    
    // افزودن به نشان‌شده‌ها
    fun add(user: User, job: JobPosting): Bookmark? {
        if (repository.existsByUserIdAndJobId(user.id, job.id)) {
            return null // قبلاً نشان شده
        }
        return repository.save(Bookmark(user = user, job = job))
    }
    
    // حذف از نشان‌شده‌ها
    @Transactional
    fun remove(userId: Long, jobId: Long): Boolean {
        if (!repository.existsByUserIdAndJobId(userId, jobId)) {
            return false
        }
        repository.deleteByUserIdAndJobId(userId, jobId)
        return true
    }
    
    // بررسی نشان‌شده بودن
    fun isBookmarked(userId: Long, jobId: Long): Boolean {
        return repository.existsByUserIdAndJobId(userId, jobId)
    }
}
