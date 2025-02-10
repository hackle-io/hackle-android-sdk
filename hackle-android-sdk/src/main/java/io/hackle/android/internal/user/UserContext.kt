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

    /**
     * 코호트를 업데이트한다.
     *
     * 현재 캐싱된 코호트에 새로운 코호트를 추가하거나 업데이트한다.
     * @param userCohorts 업데이트할 코호트
     * @return 업데이트된 UserContext
     */
    fun update(userCohorts: UserCohorts): UserContext {
        val filteredCohorts = userCohorts.filterBy(this.user)
        val newCohort = this.cohorts.toBuilder()
            .putAll(filteredCohorts)
            .build()
        return of(this.user, newCohort, this.targetEvents)
    }

    /**
     * 타겟 이벤트를 업데이트한다.
     *
     * 타겟 이벤트는 서버에서 최신의 데이터만 내려오기에 기존 것을 항상 replace 합니다.
     * @param userTargetEvents 업데이트할 타겟 이벤트
     * @return 업데이트된 UserContext
     */
    fun update(userTargetEvents: UserTargetEvents): UserContext {
        return of(this.user, this.cohorts, userTargetEvents)
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
