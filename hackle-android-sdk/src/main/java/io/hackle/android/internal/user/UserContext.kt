package io.hackle.android.internal.user

import io.hackle.sdk.common.User

internal class UserContext private constructor(
    val user: User,
    val cohorts: UserCohorts,
    val targetEvents: UserTargetEvents
) {

    fun with(user: User): UserContext {
        val filteredCohorts = this.cohorts.filterBy(user)
        return of(user, filteredCohorts, this.targetEvents)
    }

    fun update(userTarget: UserTarget): UserContext {
        val filteredCohorts = userTarget.cohorts.filterBy(this.user)
        val newCohorts = this.cohorts.toBuilder()
            .putAll(filteredCohorts)
            .build()
        val newTargetEvents = this.targetEvents.toBuilder()
            .putAll(userTarget.targetEvents)
            .build()
        return of(this.user, newCohorts, newTargetEvents)
    }

    companion object {
        fun of(user: User, cohorts: UserCohorts, targetEvents: UserTargetEvents): UserContext {
            return UserContext(
                user = user,
                cohorts = cohorts.filterBy(user),
                targetEvents = targetEvents
            )
        }
    }
}
