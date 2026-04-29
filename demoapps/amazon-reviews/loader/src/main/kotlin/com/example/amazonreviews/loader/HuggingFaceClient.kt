package com.example.amazonreviews.loader

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.jackson.*
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

class HuggingFaceClient {
    private val logger = LoggerFactory.getLogger(HuggingFaceClient::class.java)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
        engine {
            requestTimeout = 600_000 // 10 min for large files
        }
    }
    private val baseUrl = "https://huggingface.co/datasets/McAuley-Lab/Amazon-Reviews-2023"
    private val cacheDir = File(System.getProperty("java.io.tmpdir"), "amazon-reviews-cache").also { it.mkdirs() }

    suspend fun listParquetFiles(subset: String): List<String> {
        val apiUrl = "https://huggingface.co/api/datasets/McAuley-Lab/Amazon-Reviews-2023/parquet"
        logger.info("Fetching parquet file listing from $apiUrl")
        val response: Map<String, List<Map<String, Any>>> = client.get(apiUrl).body()

        val files = if (subset == "full") {
            response.values.flatten().mapNotNull { it["url"] as? String }
        } else {
            response[subset]?.mapNotNull { it["url"] as? String } ?: emptyList()
        }
        return files
    }

    suspend fun downloadFile(url: String): File {
        val fileName = url.substringAfterLast("/")
        val localFile = File(cacheDir, fileName)
        if (localFile.exists()) {
            logger.info("Using cached file: $fileName")
            return localFile
        }

        logger.info("Downloading $fileName...")
        val response: HttpResponse = client.get(url)
        val bytes: ByteArray = response.body()
        Files.write(localFile.toPath(), bytes)
        logger.info("Downloaded $fileName (${bytes.size / 1024 / 1024} MB)")
        return localFile
    }
}
