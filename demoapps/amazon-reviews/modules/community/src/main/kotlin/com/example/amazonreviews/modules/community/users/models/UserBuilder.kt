package com.example.amazonreviews.modules.community.users.models

import viaduct.api.context.ExecutionContext
import viaduct.api.grts.User as GqlUser

class UserBuilder(private val ctx: ExecutionContext) {
    fun build(user: User): GqlUser =
        GqlUser.Builder(ctx)
            .id(ctx.globalIDFor(GqlUser.Reflection, user.id))
            .displayName(user.displayName)
            .build()
}
