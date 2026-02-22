package com.iliyadev.springboot.services.site

import com.iliyadev.springboot.models.site.Slider
import com.iliyadev.springboot.repositories.site.SliderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SliderService {
    @Autowired
    lateinit var repository: SliderRepository

    private fun insert(data: Slider): Slider {
        return repository.save(data)
    }

    private fun update(data: Slider): Slider? {
        val oldData = getById(data.id)?: return null
        oldData.image = data.image
        oldData.link = data.link
        oldData.subTitle = data.subTitle
        oldData.title = data.title
        return repository.save(data)
    }

    fun getById(id: Long): Slider? {
        val data = repository.findById(id)
        if(data.isEmpty) return null
        return data.get()
    }

    fun getAll(): List<Slider> {
        return repository.findAll()
    }
    fun delete(data: Slider): Boolean {
        repository.delete(data)
        return true
    }

    fun getAllCount(): Long {
        return repository.count()
    }
}