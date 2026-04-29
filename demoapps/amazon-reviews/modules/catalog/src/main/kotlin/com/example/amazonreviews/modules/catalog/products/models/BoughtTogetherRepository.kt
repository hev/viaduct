package com.example.amazonreviews.modules.catalog.products.models

import com.example.amazonreviews.common.HevmeshClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Singleton

@Singleton
class BoughtTogetherRepository(private val hevmeshClient: HevmeshClient) {
    private val mapper = jacksonObjectMapper()

    suspend fun findByProductId(productId: String): List<String> {
        val data = hevmeshClient.getRecord("bought-together", productId.toLongOrNull() ?: return emptyList())
            ?: return emptyList()
        return mapper.readValue<List<String>>(data)
    }

    suspend fun findByProductIds(productIds: List<String>): Map<String, List<String>> {
        return productIds.associateWith { id ->
            findByProductId(id)
        }
    }
}
