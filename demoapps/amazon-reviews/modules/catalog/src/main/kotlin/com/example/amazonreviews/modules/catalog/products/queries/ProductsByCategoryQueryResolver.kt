package com.example.amazonreviews.modules.catalog.products.queries

import com.example.amazonreviews.catalog.resolverbases.QueryResolvers
import com.example.amazonreviews.modules.catalog.products.models.ProductBuilder
import com.example.amazonreviews.modules.catalog.products.models.ProductRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.ProductsConnection
import viaduct.apiannotations.ExperimentalApi

private const val DEFAULT_PAGE_SIZE = 20

@OptIn(ExperimentalApi::class)
@Resolver
class ProductsByCategoryQueryResolver
    @Inject
    constructor(
        private val productRepository: ProductRepository
    ) : QueryResolvers.ProductsByCategory() {
        override suspend fun resolve(ctx: Context): ProductsConnection? {
            val categoryId = ctx.arguments.categoryId.internalID
            val limit = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
            val cursor = ctx.arguments.after
            val page = productRepository.findByCategory(categoryId, limit, cursor)
            return ProductsConnection.Builder(ctx)
                .fromList(page.items) { ProductBuilder(ctx).build(it) }
                .build()
        }
    }
