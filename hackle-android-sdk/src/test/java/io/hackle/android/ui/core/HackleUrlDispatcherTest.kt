package io.hackle.android.ui.core

import android.content.Context
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleDelegate
import io.hackle.android.HackleUrlChannel
import io.hackle.android.HackleUrlContext
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class HackleUrlDispatcherTest {

    @Test
    fun `open - opens url with the default handler`() {
        val handler = mockk<UrlHandler>(relaxed = true)
        val sut = HackleUrlDispatcher(handler)
        val context = mockk<Context>()
        val url = mockk<Uri>()
        val extras = mockk<Bundle>()

        sut.open(context, url, HackleUrlChannel.PUSH, extras)

        verify(exactly = 1) { handler.open(context, url, HackleUrlChannel.PUSH, extras) }
    }

    @Test
    fun `setDelegate - routes links to the delegate`() {
        val defaultHandler = mockk<UrlHandler>(relaxed = true)
        val delegate = mockk<HackleDelegate>(relaxed = true)
        val sut = HackleUrlDispatcher(defaultHandler)
        val url = mockk<Uri>()

        sut.setDelegate(delegate)
        sut.open(mockk(), url, HackleUrlChannel.IN_APP_MESSAGE)

        val urlContext = slot<HackleUrlContext>()
        verify(exactly = 1) { delegate.handleUrl(capture(urlContext)) }
        expectThat(urlContext.captured.url).isSameInstanceAs(url)
        expectThat(urlContext.captured.channel).isEqualTo(HackleUrlChannel.IN_APP_MESSAGE)
        verify(exactly = 0) { defaultHandler.open(any(), any(), any(), any()) }
    }

    @Test
    fun `setDelegate - null restores the default handler`() {
        val defaultHandler = mockk<UrlHandler>(relaxed = true)
        val delegate = mockk<HackleDelegate>(relaxed = true)
        val sut = HackleUrlDispatcher(defaultHandler)

        sut.setDelegate(delegate)
        sut.setDelegate(null)
        sut.open(mockk(), mockk(), HackleUrlChannel.PUSH)

        verify(exactly = 0) { delegate.handleUrl(any()) }
        verify(exactly = 1) { defaultHandler.open(any(), any(), HackleUrlChannel.PUSH, null) }
    }
}
