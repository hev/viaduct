package com.example.amazonreviews.modules.community.users.resolvers

import com.example.amazonreviews.community.resolverbases.UserResolvers
import com.example.amazonreviews.modules.community.reviews.models.ReviewRepository
import jakarta.inject.Inject
import viaduct.api.FieldValue
import viaduct.api.Resolver

@Resolver(objectValueFragment = "fragment _ on User { id }")
class UserAverageRatingResolver @Inject constructor(
    private val reviewRepository: ReviewRepository
) : UserResolvers.AverageRating() {
    override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Double?>> {
        return contexts.map { ctx ->
            val userId = ctx.objectValue.getId().internalID
            val reviews = reviewRepository.findByUserId(userId, 1000, null)
            val avg = if (reviews.isNotEmpty()) {
                reviews.mapNotNull { it.rating }.average()
            } else {
                null
            }
            FieldValue.ofValue(avg)
        }
    }
}
