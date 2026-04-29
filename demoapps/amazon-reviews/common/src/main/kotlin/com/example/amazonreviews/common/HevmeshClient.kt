package com.example.amazonreviews.common

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

data class QueueRecord(val lsn: Long, val payload: ByteArray)

@Singleton
class HevmeshClient(@Value("\${hevmesh.base-url}") private val baseUrl: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    suspend fun getRecord(stream: String, lsn: Long): ByteArray? {
        val response = client.get("$baseUrl/streams/$stream/records/$lsn")
        return if (response.status == HttpStatusCode.OK) response.body<ByteArray>() else null
    }

    suspend fun getRecordsRange(stream: String, fromLsn: Long, toLsn: Long): List<QueueRecord> {
        return client.get("$baseUrl/streams/$stream/records") {
            parameter("from", fromLsn)
            parameter("to", toLsn)
        }.body()
    }

    suspend fun appendRecord(stream: String, payload: ByteArray): Long {
        return client.post("$baseUrl/streams/$stream/records") {
            contentType(ContentType.Application.OctetStream)
            setBody(payload)
        }.body()
    }

    suspend fun ensureStream(name: String, bufferSize: String, flushWindow: String) {
        client.put("$baseUrl/streams/$name") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("bufferSize" to bufferSize, "flushWindow" to flushWindow))
        }
    }
}
