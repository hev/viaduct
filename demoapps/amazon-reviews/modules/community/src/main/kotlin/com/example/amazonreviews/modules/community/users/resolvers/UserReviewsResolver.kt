package com.example.amazonreviews.modules.community.users.resolvers

import com.example.amazonreviews.community.resolverbases.UserResolvers
import com.example.amazonreviews.modules.community.reviews.models.ReviewBuilder
import com.example.amazonreviews.modules.community.reviews.models.ReviewRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.ReviewsConnection
import viaduct.apiannotations.ExperimentalApi

private const val DEFAULT_PAGE_SIZE = 10

@OptIn(ExperimentalApi::class)
@Resolver(objectValueFragment = "fragment _ on User { id }")
class UserReviewsResolver @Inject constructor(
    private val reviewRepository: ReviewRepository
) : UserResolvers.Reviews() {
    override suspend fun resolve(ctx: Context): ReviewsConnection? {
        val userId = ctx.objectValue.getId().internalID
        val limit = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
        val cursor = ctx.arguments.after
        val reviews = reviewRepository.findByUserId(userId, limit, cursor)
        return ReviewsConnection.Builder(ctx)
            .fromList(reviews) { ReviewBuilder(ctx).build(it) }
            .build()
    }
}
