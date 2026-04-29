package com.example.amazonreviews.modules.catalog.products.resolvers

import com.example.amazonreviews.catalog.resolverbases.ProductResolvers
import com.example.amazonreviews.modules.catalog.products.models.ProductRepository
import jakarta.inject.Inject
import viaduct.api.FieldValue
import viaduct.api.Resolver
import viaduct.api.grts.RatingSummary

@Resolver(objectValueFragment = "fragment _ on Product { id }")
class ProductRatingSummaryResolver
    @Inject
    constructor(
        private val productRepository: ProductRepository
    ) : ProductResolvers.RatingSummary() {
        override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<RatingSummary?>> {
            val productIds = contexts.map { it.objectValue.getId().internalID }
            val productsById = productRepository.findByIds(productIds)

            return contexts.map { ctx ->
                val productId = ctx.getObjectValue().getId().internalID
                val product = productsById[productId]
                if (product != null) {
                    val summary = RatingSummary.Builder(ctx)
                        .average(product.averageRating)
                        .count(product.ratingCount)
                        .distribution(emptyList())
                        .build()
                    FieldValue.ofValue(summary)
                } else {
                    FieldValue.ofValue(null)
                }
            }
        }
    }
