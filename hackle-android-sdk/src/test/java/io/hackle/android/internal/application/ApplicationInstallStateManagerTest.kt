package io.hackle.android.internal.application

import io.hackle.android.internal.application.install.ApplicationInstallDeterminer
import io.hackle.android.internal.application.install.ApplicationInstallState
import io.hackle.android.internal.application.install.ApplicationInstallStateListener
import io.hackle.android.internal.application.install.ApplicationInstallStateManager
import io.hackle.android.internal.platform.PlatformManager
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.*
import org.junit.Before
import org.junit.Test

class ApplicationInstallStateManagerTest {

    private val mockPlatformManager = mockk<PlatformManager>()
    private val mockClock = mockk<Clock>()
    private val mockInstallDeterminer = mockk<ApplicationInstallDeterminer>()
    private val mockListener = mockk<ApplicationInstallStateListener>(relaxed = true)

    private lateinit var manager: ApplicationInstallStateManager

    @Before
    fun setUp() {
        every { mockClock.currentMillis() } returns 1234567890L
        every { mockPlatformManager.currentVersion } returns PackageVersionInfo("1.0.0", 1L)
        every { mockPlatformManager.previousVersion } returns null
        every { mockPlatformManager.isDeviceIdCreated } returns false

        manager = ApplicationInstallStateManager(
            mockClock,
            mockPlatformManager,
            mockInstallDeterminer
        )
        manager.addListener(mockListener)
    }

    @Test
    fun `checkApplicationInstall should do nothing when determiner returns NONE`() {
        // given
        every { mockPlatformManager.isDeviceIdCreated } returns false
        every { mockInstallDeterminer.determine(null, any(), false) } returns ApplicationInstallState.NONE

        // when
        manager.checkApplicationInstall()

        // then
        verify(exactly = 0) { mockListener.onInstall(any(), any()) }
        verify(exactly = 0) { mockListener.onUpdate(any(), any(), any()) }
    }

    @Test
    fun `checkApplicationInstall should trigger onInstall when determiner returns INSTALL`() {
        // given
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        every { mockPlatformManager.currentVersion } returns currentVersion
        every { mockPlatformManager.previousVersion } returns null
        every { mockPlatformManager.isDeviceIdCreated } returns true
        every { mockInstallDeterminer.determine(null, currentVersion, true) } returns ApplicationInstallState.INSTALL

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onInstall(currentVersion, 1234567890L) }
        verify(exactly = 0) { mockListener.onUpdate(any(), any(), any()) }
    }

    @Test
    fun `checkApplicationInstall should trigger onUpdate when determiner returns UPDATE`() {
        // given
        val previousVersion = PackageVersionInfo("0.9.0", 0L)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        every { mockPlatformManager.previousVersion } returns previousVersion
        every { mockPlatformManager.currentVersion } returns currentVersion
        every { mockPlatformManager.isDeviceIdCreated } returns true
        every { mockInstallDeterminer.determine(previousVersion, currentVersion, true) } returns ApplicationInstallState.UPDATE

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onUpdate(previousVersion, currentVersion, 1234567890L) }
        verify(exactly = 0) { mockListener.onInstall(any(), any()) }
    }

    @Test
    fun `should handle multiple listeners for install events`() {
        // given
        val mockListener2 = mockk<ApplicationInstallStateListener>(relaxed = true)
        manager.addListener(mockListener2)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        every { mockPlatformManager.currentVersion } returns currentVersion
        every { mockPlatformManager.previousVersion } returns null
        every { mockPlatformManager.isDeviceIdCreated } returns true
        every { mockInstallDeterminer.determine(null, currentVersion, true) } returns ApplicationInstallState.INSTALL

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onInstall(currentVersion, 1234567890L) }
        verify { mockListener2.onInstall(currentVersion, 1234567890L) }
    }

    @Test
    fun `should handle multiple listeners for update events`() {
        // given
        val mockListener2 = mockk<ApplicationInstallStateListener>(relaxed = true)
        manager.addListener(mockListener2)
        val previousVersion = PackageVersionInfo("0.9.0", 0L)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        every { mockPlatformManager.previousVersion } returns previousVersion
        every { mockPlatformManager.currentVersion } returns currentVersion
        every { mockPlatformManager.isDeviceIdCreated } returns true
        every { mockInstallDeterminer.determine(previousVersion, currentVersion, true) } returns ApplicationInstallState.UPDATE

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onUpdate(previousVersion, currentVersion, 1234567890L) }
        verify { mockListener2.onUpdate(previousVersion, currentVersion, 1234567890L) }
    }

    @Test
    fun `checkApplicationInstall should not trigger any event when version is same and device id already exists`() {
        // given
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        every { mockPlatformManager.previousVersion } returns currentVersion
        every { mockPlatformManager.currentVersion } returns currentVersion
        every { mockPlatformManager.isDeviceIdCreated } returns false
        every { mockInstallDeterminer.determine(currentVersion, currentVersion, false) } returns ApplicationInstallState.NONE

        // when
        manager.checkApplicationInstall()

        // then
        verify(exactly = 0) { mockListener.onInstall(any(), any()) }
        verify(exactly = 0) { mockListener.onUpdate(any(), any(), any()) }
    }
}
