package com.example.amazonreviews.modules.catalog.products.resolvers

import com.example.amazonreviews.catalog.resolverbases.ProductResolvers
import com.example.amazonreviews.common.HevmeshClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.context.globalIDFor
import viaduct.api.grts.Review
import viaduct.api.grts.ReviewsConnection
import viaduct.apiannotations.ExperimentalApi

private const val DEFAULT_PAGE_SIZE = 10

@OptIn(ExperimentalApi::class)
@Resolver(objectValueFragment = "fragment _ on Product { id }")
class ProductReviewsResolver
    @Inject
    constructor(
        private val hevmeshClient: HevmeshClient
    ) : ProductResolvers.Reviews() {
        private val mapper = jacksonObjectMapper()

        override suspend fun resolve(ctx: Context): ReviewsConnection? {
            val productId = ctx.objectValue.getId().internalID
            val limit = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
            val from = ctx.arguments.after?.toLongOrNull() ?: 0L
            val records = hevmeshClient.getRecordsRange("product-reviews", from, from + limit)
            val reviewIds = records.mapNotNull { record ->
                try { mapper.readValue<String>(record.payload) } catch (_: Exception) { null }
            }
            val reviewRefs = reviewIds.map { id -> ctx.nodeRef(ctx.globalIDFor<Review>(id)) }
            return ReviewsConnection.Builder(ctx)
                .fromList(reviewRefs) { it }
                .build()
        }
    }
