package com.iliyadev.springboot.models.locations

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "provinces")
data class Province(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val name: String = "",  // نام استان
    
    @JsonIgnore
    @OneToMany(mappedBy = "province", cascade = [CascadeType.ALL])
    val cities: Set<City> = emptySet()
)
