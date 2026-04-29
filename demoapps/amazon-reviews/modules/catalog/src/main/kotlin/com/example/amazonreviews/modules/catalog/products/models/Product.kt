package com.example.amazonreviews.modules.catalog.products.models

data class Product(
    val id: String,
    val title: String,
    val price: Double?,
    val averageRating: Double?,
    val ratingCount: Int?,
    val description: String?,
    val imageUrls: List<String>,
    val categoryId: String?,
    val created: String?
)
