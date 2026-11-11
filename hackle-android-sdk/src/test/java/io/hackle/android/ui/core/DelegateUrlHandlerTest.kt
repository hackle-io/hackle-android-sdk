package io.hackle.android.ui.core

import android.content.Context
import android.net.Uri
import android.os.Bundle
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
import strikt.assertions.isSameInstanceAs

class DelegateUrlHandlerTest {

    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `open - in-app message link is handed to delegate without starting any activity`() {
        val delegate = mockk<HackleDelegate>(relaxed = true)
        val sut = DelegateUrlHandler(delegate)
        val url = mockk<Uri>()

        sut.open(context, url, HackleUrlChannel.IN_APP_MESSAGE, null)

        val captured = slot<HackleUrlContext>()
        verify(exactly = 1) { delegate.handleUrl(capture(captured)) }
        expectThat(captured.captured.url).isSameInstanceAs(url)
        expectThat(captured.captured.channel).isEqualTo(HackleUrlChannel.IN_APP_MESSAGE)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `open - push link is handed to delegate without starting any activity`() {
        val delegate = mockk<HackleDelegate>(relaxed = true)
        val sut = DelegateUrlHandler(delegate)
        val url = mockk<Uri>()

        sut.open(context, url, HackleUrlChannel.PUSH, mockk<Bundle>())

        val captured = slot<HackleUrlContext>()
        verify(exactly = 1) { delegate.handleUrl(capture(captured)) }
        expectThat(captured.captured.url).isSameInstanceAs(url)
        expectThat(captured.captured.channel).isEqualTo(HackleUrlChannel.PUSH)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `open - ignores exception thrown by delegate`() {
        val delegate = mockk<HackleDelegate> {
            every { handleUrl(any()) } throws IllegalStateException("fail")
        }
        val sut = DelegateUrlHandler(delegate)

        sut.open(context, mockk(), HackleUrlChannel.IN_APP_MESSAGE, null)
    }
}
