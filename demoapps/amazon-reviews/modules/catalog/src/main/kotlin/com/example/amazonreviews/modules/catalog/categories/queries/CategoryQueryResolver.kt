package com.example.amazonreviews.modules.catalog.categories.queries

import com.example.amazonreviews.catalog.resolverbases.QueryResolvers
import com.example.amazonreviews.modules.catalog.categories.models.CategoryBuilder
import com.example.amazonreviews.modules.catalog.categories.models.CategoryRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.Category

@Resolver
class CategoryQueryResolver
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository
    ) : QueryResolvers.Category() {
        override suspend fun resolve(ctx: Context): Category? {
            val id = ctx.arguments.id.internalID
            val category = categoryRepository.findById(id) ?: return null
            return CategoryBuilder(ctx).build(category)
        }
    }
