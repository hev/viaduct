package com.example.amazonreviews.modules.catalog.categories.resolvers

import com.example.amazonreviews.catalog.resolverbases.CategoryResolvers
import com.example.amazonreviews.common.HevmeshClient
import jakarta.inject.Inject
import viaduct.api.FieldValue
import viaduct.api.Resolver

@Resolver(objectValueFragment = "fragment _ on Category { id }")
class CategoryProductCountResolver
    @Inject
    constructor(
        private val hevmeshClient: HevmeshClient
    ) : CategoryResolvers.ProductCount() {
        override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Int?>> {
            return contexts.map { ctx ->
                val categoryId = ctx.getObjectValue().getId().internalID
                // Count products in category stream
                val records = hevmeshClient.getRecordsRange("category-products", 0, Long.MAX_VALUE)
                FieldValue.ofValue(records.size)
            }
        }
    }
