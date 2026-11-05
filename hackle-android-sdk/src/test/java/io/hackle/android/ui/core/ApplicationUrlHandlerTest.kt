package io.hackle.android.ui.core

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleUrlChannel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ApplicationUrlHandlerTest {

    private val sut = ApplicationUrlHandler()

    @Test
    fun `open - in-app message link starts activity without notification flags`() {
        val activity = mockk<Activity>(relaxed = true)

        sut.open(activity, mockk(), HackleUrlChannel.IN_APP_MESSAGE, null)

        verify(exactly = 1) { activity.startActivity(any()) }
    }

    @Test
    fun `open - in-app message link ignores failure`() {
        val activity = mockk<Activity> {
            every { startActivity(any()) } throws ActivityNotFoundException("no handler")
        }

        sut.open(activity, mockk(), HackleUrlChannel.IN_APP_MESSAGE, null)
    }

    @Test
    fun `open - push link falls back to launcher with the url when nothing can handle it`() {
        val url = mockk<Uri>()
        val extras = mockk<Bundle>()
        val launcherIntent = mockk<Intent>(relaxed = true)
        val context = mockk<Context> {
            every { packageName } returns "io.hackle.test"
            every { packageManager } returns mockk<PackageManager> {
                every { getLaunchIntentForPackage("io.hackle.test") } returns launcherIntent
            }
            every { startActivity(any()) } throws ActivityNotFoundException("no handler")
            every { startActivity(launcherIntent) } returns Unit
        }

        sut.open(context, url, HackleUrlChannel.PUSH, extras)

        verify(exactly = 1) { launcherIntent.setData(url) }
        verify(exactly = 1) { launcherIntent.addFlags(LinkOpener.NOTIFICATION_INTENT_FLAGS) }
        verify(exactly = 1) { context.startActivity(launcherIntent) }
    }
}
