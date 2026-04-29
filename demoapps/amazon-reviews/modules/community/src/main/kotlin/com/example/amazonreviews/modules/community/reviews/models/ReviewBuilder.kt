package com.example.amazonreviews.modules.community.reviews.models

import viaduct.api.context.ExecutionContext
import viaduct.api.grts.Review as GqlReview

class ReviewBuilder(private val ctx: ExecutionContext) {
    fun build(review: Review): GqlReview =
        GqlReview.Builder(ctx)
            .id(ctx.globalIDFor(GqlReview.Reflection, review.id))
            .rating(review.rating)
            .title(review.title)
            .text(review.text)
            .helpful(review.helpful)
            .verified(review.verified)
            .created(review.created)
            .build()
}
