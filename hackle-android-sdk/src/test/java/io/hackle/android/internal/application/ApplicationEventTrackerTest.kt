package io.hackle.android.internal.application

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.platform.model.PackageVersionInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockPackageInfo
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.user.HackleUser
import io.mockk.*
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ApplicationEventTrackerTest {

    private val userManager = mockk<UserManager>()
    private val core = mockk<HackleCore>(relaxed = true)
    private val mockUser = mockk<HackleUser>()
    private val packageInfo = MockPackageInfo(
        currentPackageVersionInfo = PackageVersionInfo("1.0.0", 1L),
        previousPackageVersionInfo = PackageVersionInfo("0.9.0", 0L)
    )

    private val tracker = ApplicationEventTracker(userManager, core, packageInfo)

    @Test
    fun `onInstall should track app install event with version info`() {
        // given
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onInstall(timestamp)

        // then
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_INSTALL_EVENT_KEY)
        expectThat(eventSlot.captured.properties["versionName"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["versionCode"]).isEqualTo(1L)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `onUpdate should track app update event with version info including previous version`() {
        // given
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onUpdate(timestamp)

        // then
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_UPDATE_EVENT_KEY)
        expectThat(eventSlot.captured.properties["versionName"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["versionCode"]).isEqualTo(1L)
        expectThat(eventSlot.captured.properties["previousVersionName"]).isEqualTo("0.9.0")
        expectThat(eventSlot.captured.properties["previousVersionCode"]).isEqualTo(0L)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `onForeground should track foreground event when state is FOREGROUND`() {
        // given
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onForeground(timestamp, false)

        // then
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)
        expectThat(eventSlot.captured.properties["isFromBackground"]).isEqualTo(false)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `onBackground should track background event when state is BACKGROUND`() {
        // given
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onBackground(timestamp)

        // then
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_BACKGROUND_EVENT_KEY)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `createEvent should handle empty version name`() {
        // given
        val packageInfoWithEmptyVersionName = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("", 1L)
        )
        val trackerWithEmptyVersion = ApplicationEventTracker(userManager, core, packageInfoWithEmptyVersionName)
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        trackerWithEmptyVersion.onInstall(timestamp)

        // then
        verify { core.track(capture(eventSlot), any(), any()) }

        expectThat(eventSlot.captured.properties["versionName"]).isEqualTo("")
        expectThat(eventSlot.captured.properties["versionCode"]).isEqualTo(1L)
    }

    @Test
    fun `createEvent should handle zero version code`() {
        // given
        val packageInfoWithZeroVersionCode = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("1.0.0", 0L)
        )
        val trackerWithZeroVersion = ApplicationEventTracker(userManager, core, packageInfoWithZeroVersionCode)
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        trackerWithZeroVersion.onInstall(timestamp)

        // then
        verify { core.track(capture(eventSlot), any(), any()) }

        expectThat(eventSlot.captured.properties["versionName"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["versionCode"]).isEqualTo(0L)
    }

    @Test
    fun `constant values should be correct`() {
        expectThat(ApplicationEventTracker.APP_INSTALL_EVENT_KEY).isEqualTo("\$app_install")
        expectThat(ApplicationEventTracker.APP_UPDATE_EVENT_KEY).isEqualTo("\$app_update")
        expectThat(ApplicationEventTracker.APP_OPEN_EVENT_KEY).isEqualTo("\$app_open")
        expectThat(ApplicationEventTracker.APP_BACKGROUND_EVENT_KEY).isEqualTo("\$app_background")
    }

    @Test
    fun `should track multiple events in sequence with different timestamps`() {
        // given
        val timestamp1 = 1000L
        val timestamp2 = 2000L
        val timestamp3 = 3000L
        val timestamp4 = 4000L
        val eventSlots = mutableListOf<Event>()
        val timestampSlots = mutableListOf<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser
        every { core.track(capture(eventSlots), any(), capture(timestampSlots)) } returns Unit

        // when - simulate full app lifecycle: install -> foreground -> background -> foreground
        tracker.onInstall(timestamp1)
        tracker.onForeground(timestamp2, false)
        tracker.onBackground(timestamp3)
        tracker.onForeground(timestamp4, true)

        // then - all events should be tracked with correct timestamps
        expectThat(eventSlots.size).isEqualTo(4)
        expectThat(eventSlots[0].key).isEqualTo(ApplicationEventTracker.APP_INSTALL_EVENT_KEY)
        expectThat(eventSlots[1].key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)
        expectThat(eventSlots[2].key).isEqualTo(ApplicationEventTracker.APP_BACKGROUND_EVENT_KEY)
        expectThat(eventSlots[3].key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)

        expectThat(timestampSlots[0]).isEqualTo(timestamp1)
        expectThat(timestampSlots[1]).isEqualTo(timestamp2)
        expectThat(timestampSlots[2]).isEqualTo(timestamp3)
        expectThat(timestampSlots[3]).isEqualTo(timestamp4)
    }

    @Test
    fun `should track update followed by foreground events`() {
        // given
        val updateTimestamp = 1000L
        val foregroundTimestamp = 1100L
        val eventSlots = mutableListOf<Event>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser
        every { core.track(capture(eventSlots), any(), any()) } returns Unit

        // when - app update followed by foreground
        tracker.onUpdate(updateTimestamp)
        tracker.onForeground(foregroundTimestamp, true)

        // then
        expectThat(eventSlots.size).isEqualTo(2)
        expectThat(eventSlots[0].key).isEqualTo(ApplicationEventTracker.APP_UPDATE_EVENT_KEY)
        expectThat(eventSlots[0].properties["versionName"]).isEqualTo("1.0.0")
        expectThat(eventSlots[0].properties["previousVersionName"]).isEqualTo("0.9.0")
        expectThat(eventSlots[1].key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)
        expectThat(eventSlots[1].properties["isFromBackground"]).isEqualTo(true)
    }

    @Test
    fun `onForeground should include isFromBackground flag correctly`() {
        // given
        val timestamp1 = 1000L
        val timestamp2 = 2000L
        val eventSlots = mutableListOf<Event>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser
        every { core.track(capture(eventSlots), any(), any()) } returns Unit

        // when - first foreground (not from background), then from background
        tracker.onForeground(timestamp1, false)
        tracker.onForeground(timestamp2, true)

        // then
        expectThat(eventSlots.size).isEqualTo(2)
        expectThat(eventSlots[0].properties["isFromBackground"]).isEqualTo(false)
        expectThat(eventSlots[1].properties["isFromBackground"]).isEqualTo(true)
    }

    @Test
    fun `should handle rapid state transitions correctly`() {
        // given
        val eventSlots = mutableListOf<Event>()
        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser
        every { core.track(capture(eventSlots), any(), any()) } returns Unit

        // when - rapid state transitions
        tracker.onForeground(1000L, false)
        tracker.onBackground(1001L)
        tracker.onForeground(1002L, true)
        tracker.onBackground(1003L)
        tracker.onForeground(1004L, true)

        // then - all events should be tracked
        expectThat(eventSlots.size).isEqualTo(5)
        expectThat(eventSlots[0].key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)
        expectThat(eventSlots[1].key).isEqualTo(ApplicationEventTracker.APP_BACKGROUND_EVENT_KEY)
        expectThat(eventSlots[2].key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)
        expectThat(eventSlots[3].key).isEqualTo(ApplicationEventTracker.APP_BACKGROUND_EVENT_KEY)
        expectThat(eventSlots[4].key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)
    }
}