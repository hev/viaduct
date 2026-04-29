package com.example.amazonreviews.modules.catalog.products.models

import com.example.amazonreviews.common.HevmeshClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Singleton

data class Page<T>(val items: List<T>, val nextCursor: String?, val totalCount: Int)

@Singleton
class ProductRepository(private val hevmeshClient: HevmeshClient) {
    private val mapper = jacksonObjectMapper()

    suspend fun findById(id: String): Product? {
        val data = hevmeshClient.getRecord("products", id.toLongOrNull() ?: return null) ?: return null
        return mapper.readValue<Product>(data)
    }

    suspend fun findByIds(ids: List<String>): Map<String, Product> {
        return ids.mapNotNull { id -> findById(id)?.let { id to it } }.toMap()
    }

    suspend fun findByCategory(categoryId: String, limit: Int, cursor: String?): Page<Product> {
        val from = cursor?.toLongOrNull() ?: 0L
        val records = hevmeshClient.getRecordsRange("category-products", from, from + limit)
        val products = records.mapNotNull { record ->
            try { mapper.readValue<Product>(record.payload) } catch (_: Exception) { null }
        }
        val nextCursor = if (records.size >= limit) records.last().lsn.toString() else null
        return Page(products, nextCursor, products.size)
    }

    suspend fun search(query: String, limit: Int): List<Product> {
        // Search is delegated to hevmesh stream scanning
        val records = hevmeshClient.getRecordsRange("products", 0, limit.toLong())
        return records.mapNotNull { record ->
            try {
                val product = mapper.readValue<Product>(record.payload)
                if (product.title.contains(query, ignoreCase = true)) product else null
            } catch (_: Exception) { null }
        }.take(limit)
    }

    suspend fun findAll(limit: Int, cursor: String?): Page<Product> {
        val from = cursor?.toLongOrNull() ?: 0L
        val records = hevmeshClient.getRecordsRange("products", from, from + limit)
        val products = records.mapNotNull { record ->
            try { mapper.readValue<Product>(record.payload) } catch (_: Exception) { null }
        }
        val nextCursor = if (records.size >= limit) records.last().lsn.toString() else null
        return Page(products, nextCursor, products.size)
    }
}
