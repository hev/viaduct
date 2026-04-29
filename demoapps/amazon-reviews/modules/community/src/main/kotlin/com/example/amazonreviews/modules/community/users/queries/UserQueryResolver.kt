package com.example.amazonreviews.modules.community.users.queries

import com.example.amazonreviews.community.resolverbases.QueryResolvers
import com.example.amazonreviews.modules.community.users.models.UserBuilder
import com.example.amazonreviews.modules.community.users.models.UserRepository
import jakarta.inject.Inject
import viaduct.api.Resolver
import viaduct.api.grts.User

@Resolver
class UserQueryResolver @Inject constructor(
    private val userRepository: UserRepository
) : QueryResolvers.User() {
    override suspend fun resolve(ctx: Context): User? {
        val id = ctx.arguments.id.internalID
        val user = userRepository.findById(id) ?: return null
        return UserBuilder(ctx).build(user)
    }
}
