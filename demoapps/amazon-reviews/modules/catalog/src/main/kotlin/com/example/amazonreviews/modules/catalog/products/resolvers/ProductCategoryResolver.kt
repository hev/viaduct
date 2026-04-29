package com.example.amazonreviews.modules.catalog.products.resolvers

import com.example.amazonreviews.catalog.resolverbases.ProductResolvers
import com.example.amazonreviews.modules.catalog.products.models.ProductRepository
import jakarta.inject.Inject
import viaduct.api.FieldValue
import viaduct.api.Resolver
import viaduct.api.context.globalIDFor
import viaduct.api.grts.Category

@Resolver(objectValueFragment = "fragment _ on Product { id }")
class ProductCategoryResolver
    @Inject
    constructor(
        private val productRepository: ProductRepository
    ) : ProductResolvers.Category() {
        override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Category?>> {
            val productIds = contexts.map { it.objectValue.getId().internalID }
            val productsById = productRepository.findByIds(productIds)

            return contexts.map { ctx ->
                val productId = ctx.getObjectValue().getId().internalID
                val product = productsById[productId]

                val categoryRef = product?.categoryId?.let {
                    ctx.nodeRef(ctx.globalIDFor<Category>(it))
                }

                if (categoryRef != null) {
                    FieldValue.ofValue(categoryRef)
                } else {
                    FieldValue.ofValue(null)
                }
            }
        }
    }
