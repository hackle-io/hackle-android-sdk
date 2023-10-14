package io.hackle.android.internal.user

import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.UserCohorts

internal interface UserCohortFetcher {
    fun fetch(user: User): UserCohorts
}


object EmptyUserCohortFetcher : UserCohortFetcher {
    private val log = Logger<EmptyUserCohortFetcher>()

    override fun fetch(user: User): UserCohorts {
        log.debug { "Fetch UserCohorts" }
        return UserCohorts.empty()
    }
}