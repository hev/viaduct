package com.example.amazonreviews.modules.community.reviews.models

data class Review(
    val id: String,
    val rating: Double?,
    val title: String?,
    val text: String?,
    val helpful: Int?,
    val verified: Boolean?,
    val created: String?,
    val authorId: String?,
    val productId: String?
)
