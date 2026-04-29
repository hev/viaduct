package com.example.amazonreviews.modules.catalog.products.models

import viaduct.api.context.ExecutionContext
import viaduct.api.grts.Product as GqlProduct

class ProductBuilder(private val ctx: ExecutionContext) {
    fun build(product: Product): GqlProduct =
        GqlProduct.Builder(ctx)
            .id(ctx.globalIDFor(GqlProduct.Reflection, product.id))
            .title(product.title)
            .price(product.price)
            .averageRating(product.averageRating)
            .ratingCount(product.ratingCount)
            .description(product.description)
            .imageUrls(product.imageUrls)
            .created(product.created)
            .build()
}
