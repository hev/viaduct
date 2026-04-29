package com.example.amazonreviews.modules.community.users.resolvers

import com.example.amazonreviews.community.resolverbases.UserResolvers
import com.example.amazonreviews.common.HevmeshClient
import jakarta.inject.Inject
import viaduct.api.FieldValue
import viaduct.api.Resolver

@Resolver(objectValueFragment = "fragment _ on User { id }")
class UserReviewCountResolver @Inject constructor(
    private val hevmeshClient: HevmeshClient
) : UserResolvers.ReviewCount() {
    override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Int?>> {
        return contexts.map { ctx ->
            val userId = ctx.objectValue.getId().internalID
            val records = hevmeshClient.getRecordsRange("user-reviews", 0, Long.MAX_VALUE)
            FieldValue.ofValue(records.size)
        }
    }
}
