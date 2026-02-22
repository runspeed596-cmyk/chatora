package com.iliyadev.springboot.models.jobs

import jakarta.persistence.*

@Entity
@Table(name = "cooperation_types")
data class CooperationType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val name: String = ""  // نوع همکاری: تمام وقت، پاره وقت، پروژه‌ای، کارآموزی، دورکاری
)
