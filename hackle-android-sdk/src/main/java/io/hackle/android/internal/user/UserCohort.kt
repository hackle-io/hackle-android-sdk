package io.hackle.android.internal.user

import io.hackle.sdk.core.model.Cohort
import io.hackle.sdk.core.model.Identifier
import java.util.Collections

internal data class UserCohort(
    val identifier: Identifier,
    val cohorts: List<Cohort>,
)

internal data class UserCohorts internal constructor(private val cohorts: Map<Identifier, UserCohort>) {

    operator fun get(identifier: Identifier): UserCohort? {
        return cohorts[identifier]
    }

    fun asList(): List<UserCohort> {
        return cohorts.values.toList()
    }

    fun asMap(): Map<Identifier, UserCohort> {
        return cohorts
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder internal constructor() {

        private val cohorts = hashMapOf<Identifier, UserCohort>()

        constructor(cohorts: UserCohorts) : this() {
            putAll(cohorts.asList())
        }

        fun put(cohort: UserCohort) = apply {
            cohorts[cohort.identifier] = cohort
        }

        fun putAll(cohorts: List<UserCohort>) = apply {
            for (cohort in cohorts) {
                put(cohort)
            }
        }

        fun putAll(cohorts: UserCohorts) = apply {
            putAll(cohorts.asList())
        }

        fun build(): UserCohorts {
            return from(Collections.unmodifiableMap(cohorts))
        }
    }

    companion object {
        private val EMPTY = UserCohorts(emptyMap())

        fun empty(): UserCohorts {
            return EMPTY
        }

        fun builder(): Builder {
            return Builder()
        }

        fun from(cohorts: Map<Identifier, UserCohort>): UserCohorts {
            if (cohorts.isEmpty()) {
                return empty()
            }
            return UserCohorts(cohorts)
        }

        fun from(dto: UserCohortsResponseDto): UserCohorts {
            return dto.cohorts.asSequence()
                .map { it.toUserCohort() }
                .fold(builder(), Builder::put)
                .build()
        }
    }
}


internal fun UserCohortDto.toUserCohort(): UserCohort {
    return UserCohort(
        identifier = Identifier(identifier.type, identifier.value),
        cohorts = cohorts.map { Cohort(it) }
    )
}
