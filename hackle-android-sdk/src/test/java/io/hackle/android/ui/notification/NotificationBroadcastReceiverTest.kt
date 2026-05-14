package io.hackle.android.ui.notification

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import io.hackle.android.internal.task.TaskExecutors
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationBroadcastReceiverTest {

    private lateinit var sut: NotificationBroadcastReceiver
    private lateinit var context: Context
    private lateinit var notificationData: NotificationData
    private lateinit var notificationManager: NotificationManagerCompat

    @Before
    fun setUp() {
        sut = NotificationBroadcastReceiver()
        context = mockk(relaxed = true)
        notificationData = mockk(relaxed = true) {
            every { showForeground } returns true
            every { notificationId } returns 42
        }

        // Run the runOnBackground async block synchronously inside onReceive.
        mockkObject(TaskExecutors)
        every { TaskExecutors.runOnBackground(any()) } answers {
            firstArg<() -> Unit>().invoke()
        }

        // Default stubs: treat the intent as a hackle intent and let NotificationData.from parse successfully.
        mockkObject(NotificationHandler.Companion)
        every { NotificationHandler.isHackleIntent(any()) } returns true
        mockkObject(NotificationData.Companion)
        every { NotificationData.from(any()) } returns notificationData

        // The number of NotificationFactory invocations indicates whether displayNotification reached the post branch.
        mockkObject(NotificationFactory)
        every { NotificationFactory.createNotification(any(), any(), any()) } returns mockk<Notification>(relaxed = true)

        // Stub NotificationManagerCompat.from so that notify(...) calls can be verified.
        notificationManager = mockk(relaxed = true)
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(any()) } returns notificationManager

        // Default the foreground check to false (app is in background).
        val keyguardManager = mockk<KeyguardManager>(relaxed = true) {
            every { isKeyguardLocked } returns false
        }
        val activityManager = mockk<ActivityManager>(relaxed = true) {
            every { runningAppProcesses } returns emptyList()
        }
        every { context.getSystemService(Context.KEYGUARD_SERVICE) } returns keyguardManager
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { context.packageName } returns "io.hackle.test"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `intent null - createNotification not called`() {
        sut.onReceive(context, null)

        verify(exactly = 0) { NotificationFactory.createNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any<Notification>()) }
    }

    @Test
    fun `non-hackle intent - createNotification not called`() {
        every { NotificationHandler.isHackleIntent(any()) } returns false
        val intent = mockk<Intent>(relaxed = true)

        sut.onReceive(context, intent)

        verify(exactly = 0) { NotificationFactory.createNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any<Notification>()) }
    }

    @Test
    fun `intent extras null - createNotification not called`() {
        val intent = mockk<Intent>(relaxed = true) {
            every { extras } returns null
        }

        sut.onReceive(context, intent)

        verify(exactly = 0) { NotificationFactory.createNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any<Notification>()) }
    }

    @Test
    fun `notification data parse error - createNotification not called`() {
        every { NotificationData.from(any()) } returns null
        val intent = mockk<Intent>(relaxed = true)

        sut.onReceive(context, intent)

        verify(exactly = 0) { NotificationFactory.createNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any<Notification>()) }
    }

    @Test
    fun `app in foreground and showForeground false - createNotification not called`() {
        every { notificationData.showForeground } returns false
        val foregroundProcess = ActivityManager.RunningAppProcessInfo().apply {
            importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            processName = "io.hackle.test"
        }
        val activityManager = mockk<ActivityManager>(relaxed = true) {
            every { runningAppProcesses } returns listOf(foregroundProcess)
        }
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        val intent = mockk<Intent>(relaxed = true)

        sut.onReceive(context, intent)

        verify(exactly = 0) { NotificationFactory.createNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any<Notification>()) }
    }

    @Test
    fun `POST_NOTIFICATIONS permission denied - createNotification not called`() {
        every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_DENIED
        val intent = mockk<Intent>(relaxed = true)

        sut.onReceive(context, intent)

        verify(exactly = 0) { NotificationFactory.createNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any<Notification>()) }
    }

    @Test
    fun `permission granted and intent valid - createNotification called`() {
        val intent = mockk<Intent>(relaxed = true)

        sut.onReceive(context, intent)

        verify(exactly = 1) { NotificationFactory.createNotification(any(), any(), notificationData) }
        verify(exactly = 1) { notificationManager.notify("hackle_notification", 42, any<Notification>()) }
    }

    // When showForeground=true, the notification must be posted even if the app is in the foreground.
    @Test
    fun `showForeground true and app in foreground - notification posted`() {
        val foregroundProcess = ActivityManager.RunningAppProcessInfo().apply {
            importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            processName = "io.hackle.test"
        }
        val activityManager = mockk<ActivityManager>(relaxed = true) {
            every { runningAppProcesses } returns listOf(foregroundProcess)
        }
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        val intent = mockk<Intent>(relaxed = true)

        sut.onReceive(context, intent)

        verify(exactly = 1) { NotificationFactory.createNotification(any(), any(), notificationData) }
        verify(exactly = 1) { notificationManager.notify("hackle_notification", 42, any<Notification>()) }
    }

    // When the keyguard is locked, isAppInForeground returns false, so the notification is posted
    // even with showForeground=false.
    @Test
    fun `keyguard locked and showForeground false - notification posted`() {
        every { notificationData.showForeground } returns false
        val keyguardManager = mockk<KeyguardManager>(relaxed = true) {
            every { isKeyguardLocked } returns true
        }
        val foregroundProcess = ActivityManager.RunningAppProcessInfo().apply {
            importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            processName = "io.hackle.test"
        }
        val activityManager = mockk<ActivityManager>(relaxed = true) {
            every { runningAppProcesses } returns listOf(foregroundProcess)
        }
        every { context.getSystemService(Context.KEYGUARD_SERVICE) } returns keyguardManager
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        val intent = mockk<Intent>(relaxed = true)

        sut.onReceive(context, intent)

        verify(exactly = 1) { NotificationFactory.createNotification(any(), any(), notificationData) }
        verify(exactly = 1) { notificationManager.notify("hackle_notification", 42, any<Notification>()) }
    }

    // The try/catch in displayNotification swallows the createNotification exception and onReceive returns normally.
    @Test
    fun `createNotification throws - notify not called and onReceive does not propagate`() {
        every { NotificationFactory.createNotification(any(), any(), any()) } throws RuntimeException("boom")
        val intent = mockk<Intent>(relaxed = true)

        sut.onReceive(context, intent)

        verify(exactly = 1) { NotificationFactory.createNotification(any(), any(), notificationData) }
        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any<Notification>()) }
    }

}
