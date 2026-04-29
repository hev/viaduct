package com.example.amazonreviews.modules.catalog.products.resolvers

import com.example.amazonreviews.catalog.resolverbases.ProductResolvers
import com.example.amazonreviews.modules.catalog.products.models.BoughtTogetherRepository
import jakarta.inject.Inject
import viaduct.api.FieldValue
import viaduct.api.Resolver
import viaduct.api.context.globalIDFor
import viaduct.api.grts.Product

@Resolver(objectValueFragment = "fragment _ on Product { id }")
class ProductBoughtTogetherResolver
    @Inject
    constructor(
        private val boughtTogetherRepository: BoughtTogetherRepository
    ) : ProductResolvers.BoughtTogether() {
        override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<List<Product?>?>> {
            val productIds = contexts.map { it.objectValue.getId().internalID }
            val boughtTogetherMap = boughtTogetherRepository.findByProductIds(productIds)

            return contexts.map { ctx ->
                val productId = ctx.getObjectValue().getId().internalID
                val relatedIds = boughtTogetherMap[productId] ?: emptyList()
                val refs = relatedIds.map { id ->
                    ctx.nodeRef(ctx.globalIDFor<Product>(id))
                }
                FieldValue.ofValue(refs)
            }
        }
    }
