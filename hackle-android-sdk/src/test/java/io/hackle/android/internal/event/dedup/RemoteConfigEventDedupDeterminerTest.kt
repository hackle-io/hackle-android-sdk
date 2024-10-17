package io.hackle.android.internal.event.dedup

import androidx.test.core.app.ApplicationProvider
import io.hackle.android.internal.event.UserEvents
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.RemoteConfigParameter
import io.hackle.sdk.core.model.ValueType
import io.hackle.sdk.core.user.HackleUser
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
class RemoteConfigEventDedupDeterminerTest {

    @Test
    fun `supports`() {

        val sut = RemoteConfigEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abcd1234", -1)
        expectThat(sut.supports(UserEvents.track("test"))).isFalse()
        expectThat(sut.supports(mockk<UserEvent.RemoteConfig>())).isTrue()
    }

    @Test
    fun `cacheKey`() {
        val sut = RemoteConfigEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abcd1234", -1)

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
        expectThat(key) isEqualTo RemoteConfigEventDedupDeterminer.Key(
            parameterId = 42,
            valueId = 320,
            decisionReason = DecisionReason.DEFAULT_RULE
        )
    }
}
