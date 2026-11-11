package io.hackle.android.ui.notification

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleApp
import io.hackle.android.HackleDelegate
import io.hackle.android.HackleUrlChannel
import io.hackle.android.HackleUrlContext
import io.hackle.android.ui.core.ApplicationUrlHandler
import io.hackle.android.ui.core.DelegateUrlHandler
import io.hackle.android.ui.core.HackleUrlDispatcher
import io.hackle.android.ui.core.LinkOpener
import io.hackle.android.ui.core.UrlHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
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

        assertEquals(expected, LinkOpener.NOTIFICATION_INTENT_FLAGS)
    }

    @Test
    fun `notification intent flags must not contain NO_HISTORY`() {
        val noHistoryBit =
            LinkOpener.NOTIFICATION_INTENT_FLAGS and Intent.FLAG_ACTIVITY_NO_HISTORY

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

        // HackleApp is initialized without delegate by default.
        mockkObject(HackleApp.Companion)
        every { HackleApp.currentInstance() } returns appWith(ApplicationUrlHandler())

        // Build the spy AFTER mockkObject so companion stubs are in place.
        sut = spyk(NotificationTrampolineActivity(), recordPrivateCalls = true)

        // Stub Activity lifecycle / context accessors used inside onCreate / trampoline.
        every { sut.finish() } returns Unit
        every { sut.startActivity(any()) } returns Unit
    }

    @After
    fun tearDown() {
        PendingNotificationLinkStorage.drain()
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

        verify(exactly = 1) { launcherIntent.addFlags(LinkOpener.NOTIFICATION_INTENT_FLAGS) }
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

        verify(exactly = 1) { launcherIntent.addFlags(LinkOpener.NOTIFICATION_INTENT_FLAGS) }
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

        // Launcher intent must have received addFlags(NOTIFICATION_INTENT_FLAGS) and the original uri.
        verify(exactly = 1) { launcherIntent.addFlags(LinkOpener.NOTIFICATION_INTENT_FLAGS) }
        verify(exactly = 1) { launcherIntent.setData(uri) }
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

    private fun appWith(urlHandler: UrlHandler): HackleApp {
        val urlDispatcher = HackleUrlDispatcher(urlHandler)
        return mockk { every { this@mockk.urlDispatcher } returns urlDispatcher }
    }

    // ------------------------------------------------------------------
    // DEEP_LINK path with delegate → delegate only, no activity started by the SDK
    // ------------------------------------------------------------------

    @Test
    fun `DEEP_LINK path with delegate - dispatches url to delegate without starting any activity`() {
        val uri = mockk<Uri>(relaxed = true)
        stubActivityIntent(uri = uri)

        every { notificationData.clickAction } returns NotificationClickAction.DEEP_LINK
        every { NotificationData.from(any()) } returns notificationData

        val delegate = mockk<HackleDelegate>(relaxed = true)
        every { HackleApp.currentInstance() } returns appWith(DelegateUrlHandler(delegate))

        val packageManager = mockk<PackageManager>(relaxed = true)
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"

        callOnCreate()

        verify(exactly = 0) { sut.startActivity(any()) }
        verify(exactly = 0) { packageManager.getLaunchIntentForPackage(any()) }
        val context = slot<HackleUrlContext>()
        verify(exactly = 1) { delegate.handleUrl(capture(context)) }
        assertEquals(uri, context.captured.url)
        assertEquals(HackleUrlChannel.PUSH, context.captured.channel)
    }

    @Test
    fun `DEEP_LINK path without delegate - opens deep link directly`() {
        val uri = mockk<Uri>(relaxed = true)
        stubActivityIntent(uri = uri)

        every { notificationData.clickAction } returns NotificationClickAction.DEEP_LINK
        every { NotificationData.from(any()) } returns notificationData

        val packageManager = mockk<PackageManager>(relaxed = true)
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"

        callOnCreate()

        verify(exactly = 1) { sut.startActivity(any()) }
        verify(exactly = 0) { packageManager.getLaunchIntentForPackage(any()) }
    }

    @Test
    fun `DEEP_LINK path before initialization opens immediately by default`() {
        val uri = mockk<Uri>(relaxed = true)
        stubActivityIntent(uri = uri)
        every { notificationData.clickAction } returns NotificationClickAction.DEEP_LINK
        every { NotificationData.from(any()) } returns notificationData
        every { HackleApp.currentInstance() } returns null
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"

        mockkObject(LinkOpener)

        callOnCreate()

        verify(exactly = 1) { LinkOpener.openLink(sut, uri, extras) }
        verify(exactly = 1) { sut.startActivity(any()) }
        verify(exactly = 0) { packageManager.getLaunchIntentForPackage(any()) }
        assertEquals(null, PendingNotificationLinkStorage.drain())
    }

    @Test
    fun `DEEP_LINK path before initialization opens immediately when defer metadata is false`() {
        val uri = mockk<Uri>(relaxed = true)
        stubActivityIntent(uri = uri)
        every { notificationData.clickAction } returns NotificationClickAction.DEEP_LINK
        every { NotificationData.from(any()) } returns notificationData
        every { HackleApp.currentInstance() } returns null
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"
        val metadata = mockk<Bundle>()
        every { metadata.containsKey("io.hackle.android.defer_push_link_until_initialized") } returns true
        every { metadata.getBoolean("io.hackle.android.defer_push_link_until_initialized", false) } returns false
        val appInfo = ApplicationInfo().apply { metaData = metadata }
        every { packageManager.getApplicationInfo("io.hackle.test", PackageManager.GET_META_DATA) } returns appInfo
        mockkObject(LinkOpener)

        callOnCreate()

        verify(exactly = 1) { LinkOpener.openLink(sut, uri, extras) }
        verify(exactly = 0) { packageManager.getLaunchIntentForPackage(any()) }
        assertEquals(null, PendingNotificationLinkStorage.drain())
    }

    @Test
    fun `DEEP_LINK path when HackleApp is not initialized - starts launcher activity and stores pending link`() {
        val uri = mockk<Uri>(relaxed = true)
        stubActivityIntent(uri = uri)

        every { notificationData.clickAction } returns NotificationClickAction.DEEP_LINK
        every { NotificationData.from(any()) } returns notificationData
        every { HackleApp.currentInstance() } returns null

        val launcherIntent = mockk<Intent>(relaxed = true)
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { packageManager.getLaunchIntentForPackage(any()) } returns launcherIntent
        every { sut.packageManager } returns packageManager
        every { sut.packageName } returns "io.hackle.test"
        val metadata = mockk<Bundle>()
        every { metadata.containsKey("io.hackle.android.defer_push_link_until_initialized") } returns true
        every { metadata.getBoolean("io.hackle.android.defer_push_link_until_initialized", false) } returns true
        val appInfo = ApplicationInfo().apply { metaData = metadata }
        every { packageManager.getApplicationInfo("io.hackle.test", PackageManager.GET_META_DATA) } returns appInfo

        callOnCreate()

        verify(exactly = 1) { sut.startActivity(any()) }
        verify(exactly = 1) { sut.startActivity(launcherIntent) }
        verify(exactly = 1) { launcherIntent.addFlags(LinkOpener.NOTIFICATION_INTENT_FLAGS) }
        val pending = PendingNotificationLinkStorage.drain()
        assertEquals(uri, pending?.uri)
        assertEquals(extras, pending?.extras)
    }
}
