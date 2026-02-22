package com.iliyadev.springboot.repositories.locations

import com.iliyadev.springboot.models.locations.Province
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProvinceRepository : JpaRepository<Province, Long> {
    fun findByNameContainingIgnoreCase(name: String): List<Province>
}
