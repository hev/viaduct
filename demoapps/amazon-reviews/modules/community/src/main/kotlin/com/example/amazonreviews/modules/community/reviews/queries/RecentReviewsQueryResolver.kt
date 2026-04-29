package com.example.amazonreviews.modules.community.reviews.queries

import com.example.amazonreviews.community.resolverbases.QueryResolvers
import com.example.amazonreviews.modules.community.reviews.models.ReviewBuilder
import com.example.amazonreviews.modules.community.reviews.models.ReviewRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.Review

private const val DEFAULT_LIMIT = 20

@Resolver
class RecentReviewsQueryResolver @Inject constructor(
    private val reviewRepository: ReviewRepository
) : QueryResolvers.RecentReviews() {
    override suspend fun resolve(ctx: Context): List<Review?>? {
        val limit = ctx.arguments.limit ?: DEFAULT_LIMIT
        return reviewRepository.findRecent(limit).map { ReviewBuilder(ctx).build(it) }
    }
}
