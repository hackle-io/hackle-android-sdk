package io.hackle.android.internal.user

import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.UserCohorts

internal class UserContext private constructor(
    val user: User,
    val cohorts: UserCohorts,
) {

    fun with(user: User): UserContext {
        val filtered = this.cohorts.filterBy(user)
        return of(user, filtered)
    }

    fun update(cohorts: UserCohorts): UserContext {
        val filtered = cohorts.filterBy(this.user)
        val newCohorts = this.cohorts.toBuilder()
            .putAll(filtered)
            .build()
        return of(this.user, newCohorts)
    }

    companion object {
        fun of(user: User, cohorts: UserCohorts): UserContext {
            return UserContext(
                user = user,
                cohorts = cohorts.filterBy(user)
            )
        }
    }
}
