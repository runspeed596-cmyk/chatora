package com.iliyadev.springboot.repositories.locations

import com.iliyadev.springboot.models.locations.City
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CityRepository : JpaRepository<City, Long> {
    fun findByProvinceId(provinceId: Long): List<City>
    fun findByNameContainingIgnoreCase(name: String): List<City>
}
