package com.example.amazonreviews.modules.community.users.models

import com.example.amazonreviews.common.HevmeshClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Singleton

@Singleton
class UserRepository(private val hevmeshClient: HevmeshClient) {
    private val mapper = jacksonObjectMapper()

    suspend fun findById(id: String): User? {
        val data = hevmeshClient.getRecord("users", id.toLongOrNull() ?: return null) ?: return null
        return mapper.readValue<User>(data)
    }

    suspend fun findByIds(ids: List<String>): Map<String, User> {
        return ids.mapNotNull { id -> findById(id)?.let { id to it } }.toMap()
    }
}
