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

    fun update(userCohorts: UserCohorts?, userTargetEvents: UserTargetEvents?): UserContext {
        val newCohorts = userCohorts?.let {
            val filteredCohorts = userCohorts.filterBy(this.user)
            this.cohorts.toBuilder()
                .putAll(filteredCohorts)
                .build()
        } ?: this.cohorts

        val newTargetEvents = userTargetEvents?.let {
            this.targetEvents.toBuilder()
                .putAll(userTargetEvents)
                .build()
        } ?: this.targetEvents

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
