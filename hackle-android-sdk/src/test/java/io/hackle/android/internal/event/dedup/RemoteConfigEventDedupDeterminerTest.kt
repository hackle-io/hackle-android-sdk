package io.hackle.android.internal.event.dedup

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.event.UserEvents
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.RemoteConfigParameter
import io.hackle.sdk.core.model.ValueType
import io.hackle.sdk.core.user.HackleUser
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class RemoteConfigEventDedupDeterminerTest {

    private lateinit var rcEventDedupRepository: MapKeyValueRepository

    @Before
    fun before() {
        rcEventDedupRepository = MapKeyValueRepository()
    }

    @Test
    fun `supports`() {

        val sut = RemoteConfigEventDedupDeterminer(rcEventDedupRepository, -1)
        expectThat(sut.supports(UserEvents.track("test"))).isFalse()
        expectThat(sut.supports(mockk<UserEvent.RemoteConfig>())).isTrue()
    }

    @Test
    fun `cacheKey`() {
        val sut = RemoteConfigEventDedupDeterminer(rcEventDedupRepository, -1)

        val event = UserEvent.RemoteConfig(
            insertId = "insertId",
            timestamp = 1,
            user = HackleUser.builder().build(),
            parameter = RemoteConfigParameter(
                id = 42,
                key = "rc",
                type = ValueType.STRING,
                identifierType = "id",
                targetRules = emptyList(),
                defaultValue = RemoteConfigParameter.Value(
                    id = 32,
                    rawValue = "default"
                )
            ),
            valueId = 320,
            decisionReason = DecisionReason.DEFAULT_RULE,
            properties = emptyMap()
        )
        val key = sut.cacheKey(event)
        expectThat(key).isEqualTo("42-320-DEFAULT_RULE")
    }
}
