package com.iliyadev.springboot.models.locations

import jakarta.persistence.*

@Entity
@Table(name = "cities")
data class City(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val name: String = "",  // نام شهر
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id")
    val province: Province? = null
)
