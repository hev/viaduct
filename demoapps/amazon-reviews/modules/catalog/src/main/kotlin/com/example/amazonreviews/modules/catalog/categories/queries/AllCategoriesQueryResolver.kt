package com.example.amazonreviews.modules.catalog.categories.queries

import com.example.amazonreviews.catalog.resolverbases.QueryResolvers
import com.example.amazonreviews.modules.catalog.categories.models.CategoryBuilder
import com.example.amazonreviews.modules.catalog.categories.models.CategoryRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.Category

@Resolver
class AllCategoriesQueryResolver
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository
    ) : QueryResolvers.AllCategories() {
        override suspend fun resolve(ctx: Context): List<Category?>? {
            return categoryRepository.findAll().map { CategoryBuilder(ctx).build(it) }
        }
    }
