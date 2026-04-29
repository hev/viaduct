package com.example.amazonreviews.modules.community.reviews.resolvers

import com.example.amazonreviews.community.resolverbases.ReviewResolvers
import com.example.amazonreviews.modules.community.reviews.models.ReviewRepository
import jakarta.inject.Inject
import viaduct.api.FieldValue
import viaduct.api.Resolver
import viaduct.api.context.globalIDFor
import viaduct.api.grts.Product

@Resolver(objectValueFragment = "fragment _ on Review { id }")
class ReviewProductResolver @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ReviewResolvers.Product() {
    override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Product?>> {
        val reviewIds = contexts.map { it.objectValue.getId().internalID }
        val reviewsById = reviewRepository.findByIds(reviewIds)

        return contexts.map { ctx ->
            val reviewId = ctx.objectValue.getId().internalID
            val review = reviewsById[reviewId]
            val productRef = review?.productId?.let {
                ctx.nodeRef(ctx.globalIDFor<Product>(it))
            }
            FieldValue.ofValue(productRef)
        }
    }
}
