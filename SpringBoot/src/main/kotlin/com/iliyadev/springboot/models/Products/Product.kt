package com.iliyadev.springboot.models.Products

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.ColumnDefault


@Entity
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val title: String = "",
    @Deprecated("Use images relationship instead")
    val image: String = "",
    val visitCount: Int = 0,
    val addDate: String = "",
    val description: String = "",
    @ColumnDefault(value = "0")
    val price: Long = 0,

    // New fields for e-commerce
    @ColumnDefault(value = "0")
    val stockQuantity: Int = 0,

    @ColumnDefault(value = "0")
    val discount: Int = 0,

    @ColumnDefault(value = "true")
    val isActive: Boolean = true,

    @ManyToMany
    val colors: Set<Color>? = null,

    @ManyToOne
    val category: ProductCategory? = null,

    @ManyToMany
    val sizes: Set<Size>? = null,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    val images: MutableList<ProductImage> = mutableListOf()
)