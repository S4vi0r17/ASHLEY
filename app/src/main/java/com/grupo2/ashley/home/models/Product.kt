package com.grupo2.ashley.home.models

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val category: String,
    val brand: String = "",
    val condition: String = "",
    val allImages: List<String> = emptyList(),
    val userId: String = "",
    val userEmail: String = "",
    val createdAt: Long = 0L,
    val deliveryLatitude: Double = 0.0,
    val deliveryLongitude: Double = 0.0
)
