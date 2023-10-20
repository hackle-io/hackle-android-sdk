package io.hackle.android.internal.user

import io.hackle.sdk.core.model.Cohort
import io.hackle.sdk.core.model.Identifier
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class UserCohortsTest {

    @Test
    fun `from`() {
        expectThat(UserCohorts.from(emptyMap())).isEqualTo(UserCohorts.empty())

        val cohort = UserCohort(Identifier("a", "b"), listOf(Cohort(42)))
        val map = mapOf(cohort.identifier to cohort)
        expectThat(UserCohorts.from(map).asMap()).isEqualTo(map)
    }

    @Test
    fun `cohorts`() {
        val cohorts = UserCohorts.builder()
            .put(UserCohort(Identifier("a", "1"), listOf(Cohort(42))))
            .putAll(
                listOf(
                    UserCohort(Identifier("b", "2"), listOf(Cohort(43)))
                )
            )
            .putAll(
                UserCohorts.from(
                    mapOf(
                        Identifier("c", "3") to UserCohort(Identifier("c", "3"), listOf(Cohort(44)))
                    )
                )
            )
            .build()

        expectThat(cohorts) {
            get { this[Identifier("a", "1")] }.isNotNull().and {
                get { this.cohorts }.hasSize(1).first().get { id }.isEqualTo(42)
            }
            get { this[Identifier("aa", "1")] }.isNull()
            get { asList() }.hasSize(3)
            get { asMap() }.hasSize(3)
        }

        val cohorts2 = cohorts.toBuilder()
            .put(UserCohort(Identifier("d", "4"), listOf(Cohort(45))))
            .build()
        expectThat(cohorts2.asList()).hasSize(4)
    }
}