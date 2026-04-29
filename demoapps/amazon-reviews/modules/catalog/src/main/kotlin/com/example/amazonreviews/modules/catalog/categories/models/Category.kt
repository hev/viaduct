package com.example.amazonreviews.modules.catalog.categories.models

data class Category(
    val id: String,
    val name: String,
    val parentId: String?,
    val childIds: List<String>
)
