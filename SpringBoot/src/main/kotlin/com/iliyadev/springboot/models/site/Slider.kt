package com.iliyadev.springboot.models.site

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id


@Entity
data class Slider(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    var image: String = "",
    var title: String = "",
    var subTitle: String = "",
    var link: String = ""
)