package io.hackle.android.ui.notification

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import io.hackle.android.HackleUrlChannel
import io.hackle.android.ui.core.HackleUrlDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class PendingNotificationLinkStorageTest {

    @After
    fun tearDown() {
        PendingNotificationLinkStorage.drain()
    }

    @Test
    fun `drain - returns null when nothing is stored`() {
        expectThat(PendingNotificationLinkStorage.drain()).isNull()
    }

    @Test
    fun `drain - returns the most recent link and clears it`() {
        val first = PendingNotificationLink(mockk(), mockk())
        val second = PendingNotificationLink(mockk(), mockk())

        PendingNotificationLinkStorage.store(first)
        PendingNotificationLinkStorage.store(second)

        expectThat(PendingNotificationLinkStorage.drain()).isSameInstanceAs(second)
        expectThat(PendingNotificationLinkStorage.drain()).isNull()
    }

    @Test
    fun `dispatch - does nothing when nothing is stored`() {
        val context = mockk<Context>(relaxed = true)
        val urlDispatcher = mockk<HackleUrlDispatcher>(relaxed = true)

        PendingNotificationLinkStorage.dispatch(context, urlDispatcher)

        verify(exactly = 0) { urlDispatcher.dispatch(any(), any()) }
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `dispatch - delegates pending link to url dispatcher`() {
        val uri = mockk<Uri>()
        val context = mockk<Context>(relaxed = true)
        val urlDispatcher = mockk<HackleUrlDispatcher> {
            every { dispatch(any(), any()) } returns true
        }
        PendingNotificationLinkStorage.store(PendingNotificationLink(uri, mockk()))

        PendingNotificationLinkStorage.dispatch(context, urlDispatcher)

        verify(exactly = 1) { urlDispatcher.dispatch(uri, HackleUrlChannel.PUSH) }
        verify(exactly = 0) { context.startActivity(any()) }
        expectThat(PendingNotificationLinkStorage.drain()).isNull()
    }

    @Test
    fun `dispatch - opens pending link when delegate is not registered`() {
        val context = mockk<Context>(relaxed = true)
        val urlDispatcher = mockk<HackleUrlDispatcher> {
            every { dispatch(any(), any()) } returns false
        }
        PendingNotificationLinkStorage.store(PendingNotificationLink(mockk(), mockk()))

        PendingNotificationLinkStorage.dispatch(context, urlDispatcher)

        verify(exactly = 1) { context.startActivity(any()) }
    }

    @Test
    fun `dispatch - ignores ActivityNotFoundException`() {
        val context = mockk<Context> {
            every { startActivity(any()) } throws ActivityNotFoundException("no handler")
        }
        val urlDispatcher = mockk<HackleUrlDispatcher> {
            every { dispatch(any(), any()) } returns false
        }
        PendingNotificationLinkStorage.store(PendingNotificationLink(mockk(), mockk()))

        PendingNotificationLinkStorage.dispatch(context, urlDispatcher)
    }
}
