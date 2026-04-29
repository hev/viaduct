package com.example.amazonreviews.modules.catalog.products.queries

import com.example.amazonreviews.catalog.resolverbases.QueryResolvers
import com.example.amazonreviews.modules.catalog.products.models.ProductBuilder
import com.example.amazonreviews.modules.catalog.products.models.ProductRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.Product

private const val DEFAULT_LIMIT = 20

@Resolver
class SearchProductsQueryResolver
    @Inject
    constructor(
        private val productRepository: ProductRepository
    ) : QueryResolvers.SearchProducts() {
        override suspend fun resolve(ctx: Context): List<Product?>? {
            val query = ctx.arguments.query
            val limit = ctx.arguments.limit ?: DEFAULT_LIMIT
            val products = productRepository.search(query, limit)
            return products.map { ProductBuilder(ctx).build(it) }
        }
    }
