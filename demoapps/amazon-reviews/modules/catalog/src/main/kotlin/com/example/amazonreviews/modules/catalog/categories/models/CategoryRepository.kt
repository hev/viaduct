package com.example.amazonreviews.modules.catalog.categories.models

import com.example.amazonreviews.common.HevmeshClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Singleton

@Singleton
class CategoryRepository(private val hevmeshClient: HevmeshClient) {
    private val mapper = jacksonObjectMapper()

    suspend fun findById(id: String): Category? {
        val data = hevmeshClient.getRecord("categories", id.toLongOrNull() ?: return null) ?: return null
        return mapper.readValue<Category>(data)
    }

    suspend fun findByIds(ids: List<String>): Map<String, Category> {
        return ids.mapNotNull { id -> findById(id)?.let { id to it } }.toMap()
    }

    suspend fun findAll(): List<Category> {
        val records = hevmeshClient.getRecordsRange("categories", 0, 10000)
        return records.mapNotNull { record ->
            try { mapper.readValue<Category>(record.payload) } catch (_: Exception) { null }
        }
    }
}
