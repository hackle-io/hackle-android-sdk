package io.hackle.android.internal.user

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.hasSize

internal class UserCohortsResponseDtoTest {

    @Test
    fun `UserCohortsResponseDto should hold list of UserCohortDto`() {
        val identifierDto = IdentifierDto("typeA", "valueA")
        val userCohortDto = UserCohortDto(identifierDto, listOf(1L, 2L, 3L))
        val responseDto = UserCohortsResponseDto(listOf(userCohortDto))

        expectThat(responseDto.cohorts).hasSize(1)
        expectThat(responseDto.cohorts.first().identifier.type).isEqualTo("typeA")
        expectThat(responseDto.cohorts.first().cohorts).isEqualTo(listOf(1L, 2L, 3L))
    }
}

internal class UserTargetResponseDtoTest {

    @Test
    fun `UserTargetResponseDto should hold list of TargetEventDto`() {
        val stat = TargetEventStatDto(date = 1234567890L, count = 10)
        val property = TargetEventPropertyDto(key = "key1", type = "string", value = "value1")
        val event = TargetEventDto(eventKey = "event1", stats = listOf(stat), property = property)
        val responseDto = UserTargetResponseDto(events = listOf(event))

        expectThat(responseDto.events).hasSize(1)
        expectThat(responseDto.events.first().eventKey).isEqualTo("event1")
        expectThat(responseDto.events.first().stats.first().count).isEqualTo(10)
        expectThat(responseDto.events.first().property).isNotNull()
        expectThat(responseDto.events.first().property?.key).isEqualTo("key1")
    }
}
