package io.hackle.android.ui.notification

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
    fun `dispatch - opens launcher with pending link when nothing can handle it`() {
        val uri = mockk<Uri>()
        val extras = mockk<Bundle>()
        val launcherIntent = mockk<Intent>(relaxed = true)
        val packageManager = mockk<PackageManager> {
            every { getLaunchIntentForPackage("io.hackle.test") } returns launcherIntent
        }
        val context = mockk<Context> {
            every { this@mockk.packageManager } returns packageManager
            every { packageName } returns "io.hackle.test"
            every { startActivity(any()) } throws ActivityNotFoundException("no handler")
            every { startActivity(launcherIntent) } returns Unit
        }
        val urlDispatcher = mockk<HackleUrlDispatcher> {
            every { dispatch(any(), any()) } returns false
        }
        PendingNotificationLinkStorage.store(PendingNotificationLink(uri, extras))

        PendingNotificationLinkStorage.dispatch(context, urlDispatcher)

        verify(exactly = 1) { launcherIntent.setData(uri) }
        verify(exactly = 1) { launcherIntent.addFlags(NotificationTrampolineActivity.NOTIFICATION_INTENT_FLAGS) }
        verify(exactly = 1) { launcherIntent.putExtras(extras) }
        verify(exactly = 1) { context.startActivity(launcherIntent) }
    }
}
