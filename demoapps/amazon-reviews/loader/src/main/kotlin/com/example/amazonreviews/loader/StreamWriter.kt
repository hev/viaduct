package com.example.amazonreviews.loader

import com.example.amazonreviews.common.HevmeshClient
import org.slf4j.LoggerFactory

class StreamWriter(hevmeshBaseUrl: String) {
    private val logger = LoggerFactory.getLogger(StreamWriter::class.java)
    private val client = HevmeshClient(hevmeshBaseUrl)

    private val streams = listOf(
        "products", "reviews", "users", "categories",
        "product-reviews", "user-reviews", "bought-together", "category-products"
    )

    suspend fun ensureStreams() {
        for (stream in streams) {
            logger.info("Ensuring stream: $stream")
            client.ensureStream(stream, bufferSize = "256MB", flushWindow = "1s")
        }
    }

    suspend fun writeRecords(records: List<LoaderRecord>) {
        for (record in records) {
            try {
                client.appendRecord(record.stream, record.payload)
            } catch (e: Exception) {
                logger.warn("Failed to write record to ${record.stream}: ${e.message}")
            }
        }
    }
}
