package io.hackle.android.internal.application

import io.hackle.android.internal.application.install.ApplicationInstallDeterminer
import io.hackle.android.internal.application.install.ApplicationInstallState
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ApplicationInstallDeterminerTest {

    @Test
    fun `determine - should return INSTALL when no previous version and device ID is created`() {
        // given
        val determiner = ApplicationInstallDeterminer(isDeviceIdCreated = true)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)

        // when
        val result = determiner.determine(currentVersion, null)

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.INSTALL)
    }

    @Test
    fun `determine - should return NONE when no previous version but device ID is not created`() {
        // given
        val determiner = ApplicationInstallDeterminer(isDeviceIdCreated = false)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)

        // when
        val result = determiner.determine(currentVersion, null)

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
    }

    @Test
    fun `determine - should return UPDATE when previous version exists and is different from current`() {
        // given
        val determiner = ApplicationInstallDeterminer(isDeviceIdCreated = false)
        val currentVersion = PackageVersionInfo("2.0.0", 2L)
        val previousVersion = PackageVersionInfo("1.0.0", 1L)

        // when
        val result = determiner.determine(currentVersion, previousVersion)

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
    }

    @Test
    fun `determine - should return NONE when previous version exists and is same as current`() {
        // given
        val determiner = ApplicationInstallDeterminer(isDeviceIdCreated = false)
        val currentVersion = PackageVersionInfo("1.0.0", 1L)
        val previousVersion = PackageVersionInfo("1.0.0", 1L)

        // when
        val result = determiner.determine(currentVersion, previousVersion)

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
    }

    @Test
    fun `determine - should return UPDATE when version name is different but version code is same`() {
        // given
        val determiner = ApplicationInstallDeterminer(isDeviceIdCreated = false)
        val currentVersion = PackageVersionInfo("1.0.1", 1L)
        val previousVersion = PackageVersionInfo("1.0.0", 1L)

        // when
        val result = determiner.determine(currentVersion, previousVersion)

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
    }

    @Test
    fun `determine - should return UPDATE when version code is different but version name is same`() {
        // given
        val determiner = ApplicationInstallDeterminer(isDeviceIdCreated = false)
        val currentVersion = PackageVersionInfo("1.0.0", 2L)
        val previousVersion = PackageVersionInfo("1.0.0", 1L)

        // when
        val result = determiner.determine(currentVersion, previousVersion)

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
    }

    @Test
    fun `determine - should return INSTALL when device ID is created even with version change`() {
        // given
        val determiner = ApplicationInstallDeterminer(isDeviceIdCreated = true)
        val currentVersion = PackageVersionInfo("2.0.0", 2L)
        val previousVersion = PackageVersionInfo("1.0.0", 1L)

        // when
        val result = determiner.determine(currentVersion, previousVersion)

        // then - when device ID is created, UPDATE takes priority
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
    }
}
