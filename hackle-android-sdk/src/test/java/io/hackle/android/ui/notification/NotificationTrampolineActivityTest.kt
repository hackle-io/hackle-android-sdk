package io.hackle.android.ui.notification

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class NotificationTrampolineActivityTest {

    @Test
    fun `notification intent flags are NEW_TASK, CLEAR_TOP, SINGLE_TOP`() {
        val expected =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

        assertEquals(expected, NotificationTrampolineActivity.NOTIFICATION_INTENT_FLAGS)
    }

    @Test
    fun `notification intent flags must not contain NO_HISTORY`() {
        val noHistoryBit =
            NotificationTrampolineActivity.NOTIFICATION_INTENT_FLAGS and Intent.FLAG_ACTIVITY_NO_HISTORY

        assertEquals(0, noHistoryBit)
    }

    // ---------------------------------------------------------------------------
    // Behavioral tests — verify that addFlags(NOTIFICATION_INTENT_FLAGS) is called
    // on the destination intent for each code path.
    // ---------------------------------------------------------------------------

    private lateinit var sut: NotificationTrampolineActivity
    private lateinit var activityIntent: Intent
    private lateinit var extras: Bundle
    private lateinit var notificationData: NotificationData
    private lateinit var notificationHandler: NotificationHandler

    @Before
    fun setUp() {
        // Stub NotificationData.from so we don't need real extras parsing.
        mockkObject(NotificationData.Companion)
        mockkObject(NotificationHandler.Companion)

        extras = mockk(relaxed = true)
        notificationData = mockk(relaxed = true)

        notificationHandler = mockk(relaxed = true)
        every { NotificationHandler.getInstance(any()) } returns notificationHandler

        // Build the spy AFTER mockkObject so companion stubs are in place.
        sut = spyk(NotificationTrampolineActivity(), recordPrivateCalls = true)

        // Stub Activity lifecycle / context accessors used inside onCreate / trampoline.
        every { sut.finish() } returns Unit
        every { sut.startActivity(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Helper: wire sut.intent to return a mock intent whose extras and data are controlled.
     */
    private fun stubActivityIntent(uri: Uri? = null): Intent {
        activityIntent = mockk(relaxed = true)
        every { activityIntent.extras } returns extras
        every { activityIntent.data } returns uri
        every { sut.intent } returns activityIntent
        return activityIntent
    }

    /**
     * Helper: invoke the protected onCreate via reflection to bypass Kotlin visibility rules.
     */
    private fun callOnCreate() {
        val method: Method = NotificationTrampolineActivity::class.java
            .getDeclaredMethod("onCreate", android.os.Bundle::class.java)
        method.isAccessible = true
        method.invoke(sut, null as android.os.Bundle?)
    }

    // ------------------------------------------------------------------
    // APP_OPEN path → startLauncherActivity → launcherIntent.addFlags(NOTIFICATION_INTENT_FLAGS)
    // ------------------------------------------------------------------

    @Test
    fun `APP_OPEN path - launcher intent receives NOTIFICATION_INTENT_FLAGS`() {
        stubActivityIntent(uri = null)

        every { notificationData.clickAction } returns NotificationClickAction.APP_OPEN
        every { NotificationData.from(any()) } returns notificationData

        val launcherIntent = mockk<Intent>(relaxed = true)
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { packageManager.getLaunchIntentForPackage(any()) } returns launcherIntent
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"

        callOnCreate()

        verify(exactly = 1) { launcherIntent.addFlags(NotificationTrampolineActivity.NOTIFICATION_INTENT_FLAGS) }
        verify(exactly = 1) { sut.startActivity(launcherIntent) }
    }

    // ------------------------------------------------------------------
    // DEEP_LINK path with uri present → Intent(ACTION_VIEW, uri).addFlags(NOTIFICATION_INTENT_FLAGS)
    // ------------------------------------------------------------------

    @Test
    fun `DEEP_LINK path with uri - startActivity called directly without going through launcher`() {
        val uri = mockk<Uri>(relaxed = true)
        stubActivityIntent(uri = uri)

        every { notificationData.clickAction } returns NotificationClickAction.DEEP_LINK
        every { NotificationData.from(any()) } returns notificationData

        // Stub packageManager as a sentinel: if getLaunchIntentForPackage is called,
        // we know startLauncherActivity was invoked (wrong path for this test).
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"

        callOnCreate()

        // On the DEEP_LINK + uri path, startActivity is called exactly once with the
        // ACTION_VIEW intent (not the launcher intent).
        verify(exactly = 1) { sut.startActivity(any()) }
        // The launcher path (packageManager) must NOT have been invoked.
        verify(exactly = 0) { packageManager.getLaunchIntentForPackage(any()) }
    }

    // ------------------------------------------------------------------
    // DEEP_LINK path with null uri → falls back to startLauncherActivity
    // ------------------------------------------------------------------

    @Test
    fun `DEEP_LINK path with null uri - launcher intent receives NOTIFICATION_INTENT_FLAGS`() {
        stubActivityIntent(uri = null)

        every { notificationData.clickAction } returns NotificationClickAction.DEEP_LINK
        every { NotificationData.from(any()) } returns notificationData

        val launcherIntent = mockk<Intent>(relaxed = true)
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { packageManager.getLaunchIntentForPackage(any()) } returns launcherIntent
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"

        callOnCreate()

        verify(exactly = 1) { launcherIntent.addFlags(NotificationTrampolineActivity.NOTIFICATION_INTENT_FLAGS) }
        verify(exactly = 1) { sut.startActivity(launcherIntent) }
    }

    // ------------------------------------------------------------------
    // DEEP_LINK path with uri but ActivityNotFoundException → falls back to launcher
    // ------------------------------------------------------------------

    @Test
    fun `DEEP_LINK path with uri and ActivityNotFoundException - launcher intent receives NOTIFICATION_INTENT_FLAGS`() {
        val uri = mockk<Uri>(relaxed = true)
        stubActivityIntent(uri = uri)

        every { notificationData.clickAction } returns NotificationClickAction.DEEP_LINK
        every { NotificationData.from(any()) } returns notificationData

        val launcherIntent = mockk<Intent>(relaxed = true)
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { packageManager.getLaunchIntentForPackage(any()) } returns launcherIntent
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"

        // First startActivity (deep link) throws ActivityNotFoundException; the launcher fallback succeeds.
        var startActivityCallCount = 0
        every { sut.startActivity(any()) } answers {
            startActivityCallCount++
            if (startActivityCallCount == 1) throw android.content.ActivityNotFoundException("no handler")
            Unit
        }

        callOnCreate()

        // Launcher intent must have received addFlags(NOTIFICATION_INTENT_FLAGS).
        verify(exactly = 1) { launcherIntent.addFlags(NotificationTrampolineActivity.NOTIFICATION_INTENT_FLAGS) }
    }

    // ------------------------------------------------------------------
    // Negative: verify NO_HISTORY bit is never in the flag value passed to addFlags
    // ------------------------------------------------------------------

    @Test
    fun `APP_OPEN path - addFlags value does not include NO_HISTORY`() {
        stubActivityIntent(uri = null)

        every { notificationData.clickAction } returns NotificationClickAction.APP_OPEN
        every { NotificationData.from(any()) } returns notificationData

        val launcherIntent = mockk<Intent>(relaxed = true)
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { packageManager.getLaunchIntentForPackage(any()) } returns launcherIntent
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"

        callOnCreate()

        // Capture the flags value actually passed and assert it has no NO_HISTORY bit.
        verify {
            launcherIntent.addFlags(match { flags ->
                (flags and Intent.FLAG_ACTIVITY_NO_HISTORY) == 0
            })
        }
    }
}
