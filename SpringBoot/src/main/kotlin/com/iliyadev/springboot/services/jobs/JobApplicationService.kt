package com.iliyadev.springboot.services.jobs

import com.iliyadev.springboot.models.enums.ApplicationStatus
import com.iliyadev.springboot.models.jobs.JobApplication
import com.iliyadev.springboot.repositories.jobs.JobApplicationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class JobApplicationService {
    
    @Autowired
    private lateinit var repository: JobApplicationRepository
    
    // ثبت درخواست کار
    fun apply(application: JobApplication): JobApplication? {
        // بررسی تکراری نبودن
        if (application.applicant?.id != null && application.job?.id != null) {
            if (repository.existsByApplicantIdAndJobId(application.applicant.id, application.job.id)) {
                return null // قبلاً درخواست داده
            }
        }
        return repository.save(application)
    }
    
    // دریافت درخواست‌های یک کارجو
    fun getByApplicant(applicantId: Long, pageIndex: Int, pageSize: Int): Page<JobApplication> {
        return repository.findByApplicantIdOrderByAppliedAtDesc(applicantId, PageRequest.of(pageIndex, pageSize))
    }
    
    // دریافت درخواست‌های یک آگهی
    fun getByJob(jobId: Long, pageIndex: Int, pageSize: Int): Page<JobApplication> {
        return repository.findByJobIdOrderByAppliedAtDesc(jobId, PageRequest.of(pageIndex, pageSize))
    }
    
    // به‌روزرسانی وضعیت درخواست
    fun updateStatus(id: Long, status: ApplicationStatus): JobApplication? {
        val application = repository.findById(id).orElse(null) ?: return null
        return repository.save(application.copy(
            status = status,
            reviewedAt = LocalDateTime.now()
        ))
    }
    
    // تعداد درخواست‌های یک آگهی
    fun countByJob(jobId: Long): Long = repository.countByJobId(jobId)
    
    // دریافت درخواست‌های کارفرما (همه آگهی‌های او)
    fun getByEmployer(employerId: Long, pageIndex: Int, pageSize: Int): Page<JobApplication> {
        return repository.findByJobEmployerIdOrderByAppliedAtDesc(employerId, PageRequest.of(pageIndex, pageSize))
    }
}

