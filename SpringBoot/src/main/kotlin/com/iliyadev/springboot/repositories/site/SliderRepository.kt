package com.iliyadev.springboot.repositories.site

import com.iliyadev.springboot.models.site.Slider
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface SliderRepository: PagingAndSortingRepository<Slider,Long>, CrudRepository<Slider, Long> {
    override fun findAll(): List<Slider>
}