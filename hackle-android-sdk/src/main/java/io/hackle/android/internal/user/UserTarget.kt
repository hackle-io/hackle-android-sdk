package io.hackle.android.internal.user

/**
 * User를 타겟팅하기 위한 정보
 * @property cohorts 사용자가 속한 Cohort
 * @property targetEvents 사용자를 타겟팅을 위해 설정한 이벤트 정보
 */
internal data class UserTarget (
    val cohorts: UserCohorts,
    val targetEvents: UserTargetEvents,
) {
    companion object {
        fun from(dto: UserTargetResponseDto): UserTarget {
            val cohorts = UserCohorts.Companion.from(dto)
            val targetEvents = UserTargetEvents.Companion.from(dto)
            return UserTarget(
                cohorts = cohorts,
                targetEvents = targetEvents,
            )
        }
    }
}
