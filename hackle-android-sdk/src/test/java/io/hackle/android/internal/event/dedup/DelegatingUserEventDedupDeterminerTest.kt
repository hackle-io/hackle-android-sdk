package io.hackle.android.internal.event.dedup

import androidx.test.core.app.ApplicationProvider
import io.hackle.android.internal.database.repository.AndroidKeyValueRepository
import io.hackle.android.internal.event.UserEvents
import io.hackle.android.internal.event.dedup.CachedUserEventDedupDeterminer.Key
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
class DelegatingUserEventDedupDeterminerTest {

    @Test
    fun `determiner`() {
        // given
        val sut = DelegatingUserEventDedupDeterminer(
            listOf(
                DeterminerStub(isSupported = false, isDedupTarget = false),
                DeterminerStub(isSupported = true, isDedupTarget = true),
                DeterminerStub(isSupported = false, isDedupTarget = false),
            )
        )

        // when
        val actual = sut.isDedupTarget(UserEvents.track("test"))

        // then
        expectThat(actual).isTrue()
    }

    @Test
    fun `empty`() {
        // given
        val sut = DelegatingUserEventDedupDeterminer(
            listOf()
        )

        // when
        val actual = sut.isDedupTarget(UserEvents.track("test"))

        // then
        expectThat(actual).isFalse()
    }

    @Test
    fun `not supported`() {
        // given
        val sut = DelegatingUserEventDedupDeterminer(
            listOf(
                DeterminerStub(isSupported = false, isDedupTarget = false),
                DeterminerStub(isSupported = false, isDedupTarget = false),
                DeterminerStub(isSupported = false, isDedupTarget = false),
            )
        )

        // when
        val actual = sut.isDedupTarget(UserEvents.track("test"))

        // then
        expectThat(actual).isFalse()
    }

    private class DeterminerStub(
        private val isSupported: Boolean,
        private val isDedupTarget: Boolean,
    ) : CachedUserEventDedupDeterminer<Key, UserEvent>(AndroidKeyValueRepository.create(ApplicationProvider.getApplicationContext(), "unittest_exposure_repo_abcd1234"), -1, Clock.SYSTEM) {

        override fun isDedupTarget(event: UserEvent): Boolean {
            return isDedupTarget
        }

        override fun supports(event: UserEvent): Boolean {
            return isSupported
        }

        override fun cacheKey(event: UserEvent): String {
            return mockk()
        }
    }
}