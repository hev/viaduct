package com.example.amazonreviews.modules.catalog.products.queries

import com.example.amazonreviews.catalog.resolverbases.QueryResolvers
import com.example.amazonreviews.modules.catalog.products.models.ProductBuilder
import com.example.amazonreviews.modules.catalog.products.models.ProductRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.Product

@Resolver
class ProductQueryResolver
    @Inject
    constructor(
        private val productRepository: ProductRepository
    ) : QueryResolvers.Product() {
        override suspend fun resolve(ctx: Context): Product? {
            val id = ctx.arguments.id.internalID
            val product = productRepository.findById(id) ?: return null
            return ProductBuilder(ctx).build(product)
        }
    }
