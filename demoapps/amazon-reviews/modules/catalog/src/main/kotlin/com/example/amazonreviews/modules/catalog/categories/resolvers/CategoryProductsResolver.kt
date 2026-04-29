package com.example.amazonreviews.modules.catalog.categories.resolvers

import com.example.amazonreviews.catalog.resolverbases.CategoryResolvers
import com.example.amazonreviews.modules.catalog.products.models.ProductBuilder
import com.example.amazonreviews.modules.catalog.products.models.ProductRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.ProductsConnection
import viaduct.apiannotations.ExperimentalApi

private const val DEFAULT_PAGE_SIZE = 20

@OptIn(ExperimentalApi::class)
@Resolver(objectValueFragment = "fragment _ on Category { id }")
class CategoryProductsResolver
    @Inject
    constructor(
        private val productRepository: ProductRepository
    ) : CategoryResolvers.Products() {
        override suspend fun resolve(ctx: Context): ProductsConnection? {
            val categoryId = ctx.objectValue.getId().internalID
            val limit = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
            val cursor = ctx.arguments.after
            val page = productRepository.findByCategory(categoryId, limit, cursor)
            return ProductsConnection.Builder(ctx)
                .fromList(page.items) { ProductBuilder(ctx).build(it) }
                .build()
        }
    }
