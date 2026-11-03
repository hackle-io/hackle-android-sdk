package io.hackle.android.ui.core

import android.net.Uri
import io.hackle.android.HackleDelegate
import io.hackle.android.HackleUrlChannel
import io.hackle.android.HackleUrlContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue

class HackleUrlDispatcherTest {

    @Test
    fun `dispatch - returns false when delegate is not registered`() {
        val sut = HackleUrlDispatcher(null)

        val actual = sut.dispatch(mockk(), HackleUrlChannel.PUSH)

        expectThat(actual).isFalse()
        expectThat(sut.hasDelegate).isFalse()
    }

    @Test
    fun `dispatch - delegates url with channel to initial delegate`() {
        val delegate = mockk<HackleDelegate>(relaxed = true)
        val sut = HackleUrlDispatcher(delegate)
        val url = mockk<Uri>()

        val actual = sut.dispatch(url, HackleUrlChannel.IN_APP_MESSAGE)

        expectThat(actual).isTrue()
        val context = slot<HackleUrlContext>()
        verify(exactly = 1) { delegate.handleUrl(capture(context)) }
        expectThat(context.captured.url).isSameInstanceAs(url)
        expectThat(context.captured.channel).isEqualTo(HackleUrlChannel.IN_APP_MESSAGE)
    }

    @Test
    fun `setDelegate - replaces delegate`() {
        val first = mockk<HackleDelegate>(relaxed = true)
        val second = mockk<HackleDelegate>(relaxed = true)
        val sut = HackleUrlDispatcher(first)

        sut.setDelegate(second)
        sut.dispatch(mockk(), HackleUrlChannel.PUSH)

        verify(exactly = 0) { first.handleUrl(any()) }
        verify(exactly = 1) { second.handleUrl(any()) }
    }

    @Test
    fun `setDelegate - removes delegate when null`() {
        val delegate = mockk<HackleDelegate>(relaxed = true)
        val sut = HackleUrlDispatcher(delegate)

        sut.setDelegate(null)
        val actual = sut.dispatch(mockk(), HackleUrlChannel.PUSH)

        expectThat(actual).isFalse()
        expectThat(sut.hasDelegate).isFalse()
        verify(exactly = 0) { delegate.handleUrl(any()) }
    }

    @Test
    fun `dispatch - returns true even if delegate throws`() {
        val delegate = mockk<HackleDelegate> {
            every { handleUrl(any()) } throws IllegalStateException("fail")
        }
        val sut = HackleUrlDispatcher(delegate)

        val actual = sut.dispatch(mockk(), HackleUrlChannel.PUSH)

        expectThat(actual).isTrue()
    }
}
