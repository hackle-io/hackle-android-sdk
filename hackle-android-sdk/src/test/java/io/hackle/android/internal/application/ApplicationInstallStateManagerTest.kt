package io.hackle.android.internal.application

import io.hackle.android.internal.application.install.ApplicationInstallDeterminer
import io.hackle.android.internal.application.install.ApplicationInstallState
import io.hackle.android.internal.application.install.ApplicationInstallStateListener
import io.hackle.android.internal.application.install.ApplicationInstallStateManager
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.platform.packageinfo.PackageInfo
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.*
import org.junit.Before
import org.junit.Test

class ApplicationInstallStateManagerTest {

    private val mockClock = mockk<Clock>()
    private val mockPackageInfo = mockk<PackageInfo>()
    private val mockKeyValueRepository = mockk<KeyValueRepository>(relaxed = true)
    private val mockInstallDeterminer = mockk<ApplicationInstallDeterminer>()
    private val mockListener = mockk<ApplicationInstallStateListener>(relaxed = true)

    private lateinit var manager: ApplicationInstallStateManager

    @Before
    fun setUp() {
        every { mockClock.currentMillis() } returns 1234567890L
        every { mockPackageInfo.packageVersionInfo } returns PackageVersionInfo("1.0.0", 1L)
        every { mockKeyValueRepository.getString(any()) } returns null
        every { mockKeyValueRepository.getLong(any(), any()) } returns Long.MIN_VALUE

        manager = ApplicationInstallStateManager(
            mockClock,
            mockPackageInfo,
            mockKeyValueRepository,
            mockInstallDeterminer
        )
        manager.addListener(mockListener)
        manager.initialize()
    }

    @Test
    fun `checkApplicationInstall should do nothing when determiner returns NONE`() {
        // given
        every { mockInstallDeterminer.determine(any(), any()) } returns ApplicationInstallState.NONE

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
        every { mockPackageInfo.packageVersionInfo } returns currentVersion
        every { mockInstallDeterminer.determine(currentVersion, null) } returns ApplicationInstallState.INSTALL

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onInstall(currentVersion, 1234567890L) }
        verify(exactly = 0) { mockListener.onUpdate(any(), any(), any()) }
        verify { mockKeyValueRepository.putString(any(), "1.0.0") }
        verify { mockKeyValueRepository.putLong(any(), 1L) }
    }

    @Test
    fun `checkApplicationInstall should trigger onUpdate when determiner returns UPDATE`() {
        // given
        val previousVersion = PackageVersionInfo("0.9.0", 0L)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        every { mockKeyValueRepository.getString(any()) } returns "0.9.0"
        every { mockKeyValueRepository.getLong(any(), any()) } returns 0L
        every { mockPackageInfo.packageVersionInfo } returns currentVersion
        every { mockInstallDeterminer.determine(currentVersion, previousVersion) } returns ApplicationInstallState.UPDATE

        manager.initialize()

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onUpdate(previousVersion, currentVersion, 1234567890L) }
        verify(exactly = 0) { mockListener.onInstall(any(), any()) }
        verify { mockKeyValueRepository.putString(any(), "1.0.0") }
        verify { mockKeyValueRepository.putLong(any(), 1L) }
    }

    @Test
    fun `should handle multiple listeners for install events`() {
        // given
        val mockListener2 = mockk<ApplicationInstallStateListener>(relaxed = true)
        manager.addListener(mockListener2)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        every { mockPackageInfo.packageVersionInfo } returns currentVersion
        every { mockInstallDeterminer.determine(currentVersion, null) } returns ApplicationInstallState.INSTALL

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
        every { mockKeyValueRepository.getString(any()) } returns "0.9.0"
        every { mockKeyValueRepository.getLong(any(), any()) } returns 0L
        every { mockPackageInfo.packageVersionInfo } returns currentVersion
        every { mockInstallDeterminer.determine(currentVersion, previousVersion) } returns ApplicationInstallState.UPDATE

        manager.initialize()

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onUpdate(previousVersion, currentVersion, 1234567890L) }
        verify { mockListener2.onUpdate(previousVersion, currentVersion, 1234567890L) }
    }

    @Test
    fun `initialize should load previous version from key value repository`() {
        // given
        every { mockKeyValueRepository.getString(any()) } returns "0.9.0"
        every { mockKeyValueRepository.getLong(any(), any()) } returns 0L

        val newManager = ApplicationInstallStateManager(
            mockClock,
            mockPackageInfo,
            mockKeyValueRepository,
            mockInstallDeterminer
        )

        // when
        newManager.initialize()

        // then
        verify { mockKeyValueRepository.getString(any()) }
        verify { mockKeyValueRepository.getLong(any(), any()) }
    }

    @Test
    fun `checkApplicationInstall should save current version after determining install state`() {
        // given
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        every { mockPackageInfo.packageVersionInfo } returns currentVersion
        every { mockInstallDeterminer.determine(any(), any()) } returns ApplicationInstallState.NONE

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockKeyValueRepository.putString(any(), "1.0.0") }
        verify { mockKeyValueRepository.putLong(any(), 1L) }
    }
}
