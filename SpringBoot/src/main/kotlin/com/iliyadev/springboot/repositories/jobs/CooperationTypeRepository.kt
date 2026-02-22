package com.iliyadev.springboot.repositories.jobs

import com.iliyadev.springboot.models.jobs.CooperationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CooperationTypeRepository : JpaRepository<CooperationType, Long>
