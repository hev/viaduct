package com.example.amazonreviews.modules.catalog.products.models

data class RatingSummary(
    val average: Double?,
    val count: Int?,
    val distribution: List<Int>
)
