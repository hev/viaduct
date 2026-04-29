package com.example.amazonreviews.modules.community.reviews.queries

import com.example.amazonreviews.community.resolverbases.QueryResolvers
import com.example.amazonreviews.modules.community.reviews.models.ReviewBuilder
import com.example.amazonreviews.modules.community.reviews.models.ReviewRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.Review

@Resolver
class ReviewQueryResolver @Inject constructor(
    private val reviewRepository: ReviewRepository
) : QueryResolvers.Review() {
    override suspend fun resolve(ctx: Context): Review? {
        val id = ctx.arguments.id.internalID
        val review = reviewRepository.findById(id) ?: return null
        return ReviewBuilder(ctx).build(review)
    }
}
