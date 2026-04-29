package com.example.amazonreviews.modules.community.reviews.models

import com.example.amazonreviews.common.HevmeshClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Singleton

@Singleton
class ReviewRepository(private val hevmeshClient: HevmeshClient) {
    private val mapper = jacksonObjectMapper()

    suspend fun findById(id: String): Review? {
        val data = hevmeshClient.getRecord("reviews", id.toLongOrNull() ?: return null) ?: return null
        return mapper.readValue<Review>(data)
    }

    suspend fun findByIds(ids: List<String>): Map<String, Review> {
        return ids.mapNotNull { id -> findById(id)?.let { id to it } }.toMap()
    }

    suspend fun findRecent(limit: Int): List<Review> {
        val records = hevmeshClient.getRecordsRange("reviews", 0, limit.toLong())
        return records.mapNotNull { record ->
            try { mapper.readValue<Review>(record.payload) } catch (_: Exception) { null }
        }
    }

    suspend fun findByProductId(productId: String, limit: Int, cursor: String?): List<Review> {
        val from = cursor?.toLongOrNull() ?: 0L
        val records = hevmeshClient.getRecordsRange("product-reviews", from, from + limit)
        return records.mapNotNull { record ->
            try { mapper.readValue<Review>(record.payload) } catch (_: Exception) { null }
        }
    }

    suspend fun findByUserId(userId: String, limit: Int, cursor: String?): List<Review> {
        val from = cursor?.toLongOrNull() ?: 0L
        val records = hevmeshClient.getRecordsRange("user-reviews", from, from + limit)
        return records.mapNotNull { record ->
            try { mapper.readValue<Review>(record.payload) } catch (_: Exception) { null }
        }
    }
}
