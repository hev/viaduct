package com.example.amazonreviews.modules.catalog.categories.resolvers

import com.example.amazonreviews.catalog.resolverbases.CategoryResolvers
import com.example.amazonreviews.modules.catalog.categories.models.CategoryRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.context.globalIDFor
import viaduct.api.grts.Category

@Resolver(objectValueFragment = "fragment _ on Category { id }")
class CategoryParentResolver
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository
    ) : CategoryResolvers.Parent() {
        override suspend fun resolve(ctx: Context): Category? {
            val categoryId = ctx.objectValue.getId().internalID
            val category = categoryRepository.findById(categoryId) ?: return null
            val parentId = category.parentId ?: return null
            return ctx.nodeRef(ctx.globalIDFor<Category>(parentId))
        }
    }
