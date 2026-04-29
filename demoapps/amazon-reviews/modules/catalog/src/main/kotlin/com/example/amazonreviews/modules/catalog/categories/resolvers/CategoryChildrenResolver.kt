package com.example.amazonreviews.modules.catalog.categories.resolvers

import com.example.amazonreviews.catalog.resolverbases.CategoryResolvers
import com.example.amazonreviews.modules.catalog.categories.models.CategoryRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.context.globalIDFor
import viaduct.api.grts.Category

@Resolver(objectValueFragment = "fragment _ on Category { id }")
class CategoryChildrenResolver
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository
    ) : CategoryResolvers.Children() {
        override suspend fun resolve(ctx: Context): List<Category?>? {
            val categoryId = ctx.objectValue.getId().internalID
            val category = categoryRepository.findById(categoryId) ?: return null
            return category.childIds.map { id ->
                ctx.nodeRef(ctx.globalIDFor<Category>(id))
            }
        }
    }
