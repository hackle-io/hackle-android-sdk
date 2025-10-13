package io.hackle.android.internal.application

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
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

    @Test
    fun `onInstall should track install event when no previous version exists`() {
        // given - 신규 설치 시나리오: 이전 버전이 없음
        val tracker = ApplicationEventTracker(userManager, core)
        val versionInfo = PackageVersionInfo("1.0.0", 1L)
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onInstall(versionInfo, timestamp)

        // then - 설치 이벤트는 현재 버전 정보만 포함
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_INSTALL_EVENT_KEY)
        expectThat(eventSlot.captured.properties["version_name"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["version_code"]).isEqualTo(1L)
        expectThat(eventSlot.captured.properties.containsKey("previous_version_name")).isEqualTo(false)
        expectThat(eventSlot.captured.properties.containsKey("previous_version_code")).isEqualTo(false)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `onUpdate should track update event with version change`() {
        // given - 업데이트 시나리오: 0.9.0 → 1.0.0 버전 업그레이드
        val tracker = ApplicationEventTracker(userManager, core)
        val previousVersion = PackageVersionInfo("0.9.0", 0L)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()
        val userSlot = slot<HackleUser>()
        val timestampSlot = slot<Long>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onUpdate(previousVersion, currentVersion, timestamp)

        // then - 업데이트 이벤트는 현재 버전과 이전 버전 모두 포함
        verify { core.track(capture(eventSlot), capture(userSlot), capture(timestampSlot)) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_UPDATE_EVENT_KEY)
        expectThat(eventSlot.captured.properties["version_name"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["version_code"]).isEqualTo(1L)
        expectThat(eventSlot.captured.properties["previous_version_name"]).isEqualTo("0.9.0")
        expectThat(eventSlot.captured.properties["previous_version_code"]).isEqualTo(0L)
        expectThat(userSlot.captured).isEqualTo(mockUser)
        expectThat(timestampSlot.captured).isEqualTo(timestamp)
    }

    @Test
    fun `onUpdate should handle null previousVersionInfo`() {
        // given - edge case: 업데이트로 처리되지만 이전 버전 정보가 없는 경우
        val tracker = ApplicationEventTracker(userManager, core)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onUpdate(null, currentVersion, timestamp)

        // then - previousVersion이 null로 설정됨
        verify { core.track(capture(eventSlot), any(), any()) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_UPDATE_EVENT_KEY)
        expectThat(eventSlot.captured.properties["version_name"]).isEqualTo("1.0.0")
        expectThat(eventSlot.captured.properties["version_code"]).isEqualTo(1L)
        expectThat(eventSlot.captured.properties["previous_version_name"]).isEqualTo(null)
        expectThat(eventSlot.captured.properties["previous_version_code"]).isEqualTo(null)
    }

    @Test
    fun `onForeground should track app open event`() {
        // given
        val tracker = ApplicationEventTracker(userManager, core)
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onForeground(timestamp, true)

        // then
        verify { core.track(capture(eventSlot), any(), any()) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_OPEN_EVENT_KEY)
        expectThat(eventSlot.captured.properties["is_from_background"]).isEqualTo(true)
    }

    @Test
    fun `onBackground should track app background event`() {
        // given
        val tracker = ApplicationEventTracker(userManager, core)
        val timestamp = 1234567890L
        val eventSlot = slot<Event>()

        every { userManager.resolve(null, HackleAppContext.DEFAULT) } returns mockUser

        // when
        tracker.onBackground(timestamp)

        // then
        verify { core.track(capture(eventSlot), any(), any()) }

        expectThat(eventSlot.captured.key).isEqualTo(ApplicationEventTracker.APP_BACKGROUND_EVENT_KEY)
    }
}
