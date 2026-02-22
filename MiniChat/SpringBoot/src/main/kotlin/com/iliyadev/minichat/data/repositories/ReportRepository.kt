package com.iliyadev.minichat.data.repositories

import com.iliyadev.minichat.domain.entities.Report
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReportRepository : JpaRepository<Report, UUID>
