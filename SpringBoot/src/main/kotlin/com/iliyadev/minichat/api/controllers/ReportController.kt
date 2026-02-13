package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.core.response.ApiResponse
import com.iliyadev.minichat.domain.entities.Report
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import com.iliyadev.minichat.data.repositories.ReportRepository
import org.springframework.web.bind.annotation.*
import java.security.Principal

data class ReportRequest(
    val reportedUserId: String,
    val reason: String,
    val description: String?
)

@RestController
@RequestMapping("/reports")
class ReportController(
    private val reportRepository: ReportRepository
) {

    @PostMapping
    fun submitReport(@RequestBody request: ReportRequest, principal: Principal): ApiResponse<String> {
        val report = Report(
            reporterId = principal.name,
            reportedUserId = request.reportedUserId,
            reason = request.reason,
            description = request.description
        )
        reportRepository.save(report)
        // Todo: Trigger Abuse Detection limit check
        return ApiResponse.success("Report submitted successfully")
    }
}
