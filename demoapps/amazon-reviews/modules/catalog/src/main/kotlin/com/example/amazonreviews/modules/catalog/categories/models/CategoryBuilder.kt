package com.example.amazonreviews.modules.catalog.categories.models

import viaduct.api.context.ExecutionContext
import viaduct.api.grts.Category as GqlCategory

class CategoryBuilder(private val ctx: ExecutionContext) {
    fun build(category: Category): GqlCategory =
        GqlCategory.Builder(ctx)
            .id(ctx.globalIDFor(GqlCategory.Reflection, category.id))
            .name(category.name)
            .build()
}
