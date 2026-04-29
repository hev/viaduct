package com.example.amazonreviews.modules.catalog.products.resolvers

import com.example.amazonreviews.catalog.resolverbases.ProductResolvers
import com.example.amazonreviews.common.HevmeshClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.context.globalIDFor
import viaduct.api.grts.Review

private const val DEFAULT_LIMIT = 5

@Resolver(objectValueFragment = "fragment _ on Product { id }")
class ProductTopReviewsResolver
    @Inject
    constructor(
        private val hevmeshClient: HevmeshClient
    ) : ProductResolvers.TopReviews() {
        private val mapper = jacksonObjectMapper()

        override suspend fun resolve(ctx: Context): List<Review?>? {
            val productId = ctx.objectValue.getId().internalID
            val limit = ctx.arguments.limit ?: DEFAULT_LIMIT
            val records = hevmeshClient.getRecordsRange("product-reviews", 0, limit.toLong())
            val reviewIds = records.mapNotNull { record ->
                try { mapper.readValue<String>(record.payload) } catch (_: Exception) { null }
            }
            return reviewIds.map { id -> ctx.nodeRef(ctx.globalIDFor<Review>(id)) }
        }
    }
