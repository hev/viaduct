package com.example.amazonreviews.loader

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.slf4j.LoggerFactory
import java.io.File

data class LoaderRecord(
    val stream: String,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

class ParquetReader {
    private val logger = LoggerFactory.getLogger(ParquetReader::class.java)
    private val mapper = jacksonObjectMapper()

    fun readRecords(file: File): List<LoaderRecord> {
        val records = mutableListOf<LoaderRecord>()
        val conf = Configuration()
        val path = Path(file.absolutePath)
        val inputFile = HadoopInputFile.fromPath(path, conf)

        AvroParquetReader.builder<GenericRecord>(inputFile).build().use { reader ->
            var record = reader.read()
            while (record != null) {
                try {
                    records.addAll(transformRecord(record))
                } catch (e: Exception) {
                    logger.warn("Failed to transform record: ${e.message}")
                }
                record = reader.read()
            }
        }

        return records
    }

    private fun transformRecord(record: GenericRecord): List<LoaderRecord> {
        val results = mutableListOf<LoaderRecord>()
        val schema = record.schema.name

        when {
            record.hasField("parent_asin") || record.hasField("asin") -> {
                // This is a review record
                val reviewMap = mapOf(
                    "id" to (record.get("review_id")?.toString() ?: return results),
                    "rating" to record.get("rating"),
                    "title" to record.get("title")?.toString(),
                    "text" to record.get("text")?.toString(),
                    "helpful" to (record.get("helpful_vote") ?: 0),
                    "verified" to (record.get("verified_purchase") ?: false),
                    "created" to record.get("timestamp")?.toString(),
                    "authorId" to record.get("user_id")?.toString(),
                    "productId" to (record.get("parent_asin") ?: record.get("asin"))?.toString()
                )
                results.add(LoaderRecord("reviews", mapper.writeValueAsBytes(reviewMap)))

                // Index: user -> review
                record.get("user_id")?.toString()?.let { userId ->
                    results.add(LoaderRecord("user-reviews", mapper.writeValueAsBytes(
                        mapOf("userId" to userId, "reviewId" to reviewMap["id"])
                    )))
                }

                // Index: product -> review
                (record.get("parent_asin") ?: record.get("asin"))?.toString()?.let { productId ->
                    results.add(LoaderRecord("product-reviews", mapper.writeValueAsBytes(
                        mapOf("productId" to productId, "reviewId" to reviewMap["id"])
                    )))
                }

                // Upsert user
                record.get("user_id")?.toString()?.let { userId ->
                    val userMap = mapOf("id" to userId, "displayName" to userId)
                    results.add(LoaderRecord("users", mapper.writeValueAsBytes(userMap)))
                }
            }
            record.hasField("main_category") -> {
                // This is a product/item metadata record
                val productMap = mapOf(
                    "id" to (record.get("parent_asin") ?: record.get("asin"))?.toString(),
                    "title" to record.get("title")?.toString(),
                    "price" to record.get("price"),
                    "averageRating" to record.get("average_rating"),
                    "ratingCount" to record.get("rating_number"),
                    "description" to (record.get("description") as? List<*>)?.joinToString(" "),
                    "imageUrls" to (record.get("images") as? List<*>)?.map { it.toString() }.orEmpty(),
                    "categoryId" to record.get("main_category")?.toString(),
                    "created" to record.get("timestamp")?.toString()
                )
                results.add(LoaderRecord("products", mapper.writeValueAsBytes(productMap)))

                // Index: category -> product
                record.get("main_category")?.toString()?.let { category ->
                    val catMap = mapOf("id" to category, "name" to category, "parentId" to null, "childIds" to emptyList<String>())
                    results.add(LoaderRecord("categories", mapper.writeValueAsBytes(catMap)))
                    results.add(LoaderRecord("category-products", mapper.writeValueAsBytes(
                        mapOf("categoryId" to category, "productId" to productMap["id"])
                    )))
                }

                // Co-purchase graph
                (record.get("bought_together") as? List<*>)?.let { related ->
                    if (related.isNotEmpty()) {
                        results.add(LoaderRecord("bought-together", mapper.writeValueAsBytes(
                            mapOf("productId" to productMap["id"], "relatedIds" to related.map { it.toString() })
                        )))
                    }
                }
            }
        }
        return results
    }

    private fun GenericRecord.hasField(name: String): Boolean {
        return schema.getField(name) != null
    }
}
