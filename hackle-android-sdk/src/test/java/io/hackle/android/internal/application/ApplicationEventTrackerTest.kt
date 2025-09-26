package io.hackle.android.internal.application

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.platform.model.PackageInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockDevice
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class ApplicationEventTrackerTest {

    private val userManager = mockk<UserManager>()
    private val core = mockk<HackleCore>(relaxed = true)
    private val mockUser = mockk<HackleUser>()
    private val packageInfo = PackageInfo("test.app", "1.0.0", 1L, "0.9.0", 0L)
    private val device = MockDevice(
        id = "test-device",
        properties = emptyMap(),
        packageInfo = packageInfo
    )

    private val tracker = ApplicationEventTracker(userManager, core, device)

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
    fun `onOpen should track app open event with version info`() {
        // given
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onOpen(timestamp)

        // then
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)
        expectThat(eventSlot.captured.properties["versionName"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["versionCode"]).isEqualTo(1L)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `onState should track foreground event when state is FOREGROUND`() {
        // given
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onState(AppState.FOREGROUND, timestamp)

        // then
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_FOREGROUND_EVENT_KEY)
        expectThat(eventSlot.captured.properties["versionName"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["versionCode"]).isEqualTo(1L)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `onState should track background event when state is BACKGROUND`() {
        // given
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onState(AppState.BACKGROUND, timestamp)

        // then
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_BACKGROUND_EVENT_KEY)
        expectThat(eventSlot.captured.properties["versionName"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["versionCode"]).isEqualTo(1L)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `createEvent should handle empty version name`() {
        // given
        val deviceWithEmptyVersionName = MockDevice(
            id = "test-device",
            properties = emptyMap(),
            packageInfo = PackageInfo("test.app", "", 1L, null, null)
        )
        val trackerWithEmptyVersion = ApplicationEventTracker(userManager, core, deviceWithEmptyVersionName)
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
        val deviceWithZeroVersionCode = MockDevice(
            id = "test-device",
            properties = emptyMap(),
            packageInfo = PackageInfo("test.app", "1.0.0", 0L, null, null)
        )
        val trackerWithZeroVersion = ApplicationEventTracker(userManager, core, deviceWithZeroVersionCode)
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
        expectThat(ApplicationEventTracker.APP_FOREGROUND_EVENT_KEY).isEqualTo("\$app_foreground")
        expectThat(ApplicationEventTracker.APP_BACKGROUND_EVENT_KEY).isEqualTo("\$app_background")
    }
}