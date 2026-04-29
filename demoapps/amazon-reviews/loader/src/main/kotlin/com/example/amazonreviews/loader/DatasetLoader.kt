package com.example.amazonreviews.loader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DatasetLoader")

fun main() = runBlocking {
    val hevmeshBaseUrl = System.getenv("HEVMESH_BASE_URL") ?: "http://localhost:9090"
    val datasetSubset = System.getenv("DATASET_SUBSET") ?: "full"
    val parallelism = System.getenv("PARALLELISM")?.toIntOrNull() ?: 8

    logger.info("Starting Amazon Reviews data loader")
    logger.info("  hevmesh: $hevmeshBaseUrl")
    logger.info("  dataset: $datasetSubset")
    logger.info("  parallelism: $parallelism")

    val streamWriter = StreamWriter(hevmeshBaseUrl)
    val huggingFaceClient = HuggingFaceClient()
    val parquetReader = ParquetReader()

    // Ensure streams exist
    streamWriter.ensureStreams()

    // Download parquet file listing
    val parquetFiles = huggingFaceClient.listParquetFiles(datasetSubset)
    logger.info("Found ${parquetFiles.size} parquet files to process")

    // Process files in parallel batches
    coroutineScope {
        parquetFiles.chunked(parallelism).forEach { batch ->
            batch.map { fileUrl ->
                async(Dispatchers.IO) {
                    try {
                        val localPath = huggingFaceClient.downloadFile(fileUrl)
                        val records = parquetReader.readRecords(localPath)
                        streamWriter.writeRecords(records)
                        logger.info("Processed $fileUrl: ${records.size} records")
                    } catch (e: Exception) {
                        logger.error("Failed to process $fileUrl", e)
                    }
                }
            }.awaitAll()
        }
    }

    logger.info("Data loading complete")
}
