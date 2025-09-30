package io.hackle.android.internal.application

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.model.PackageInfo
import io.hackle.android.internal.platform.model.PackageVersionInfo
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ApplicationInstallDeterminerTest {

    private val keyValueRepository = mockk<KeyValueRepository>(relaxed = true)

    @Test
    fun `determine - should return INSTALL when no previous version and device ID is created`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = true
        )
        val packageInfo = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("1.0.0", 1L),
            previousPackageVersionInfo = null
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device, packageInfo)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.INSTALL)
        verify { keyValueRepository.putString(PackageInfo.KEY_PREVIOUS_VERSION_NAME, "1.0.0") }
        verify { keyValueRepository.putLong(PackageInfo.KEY_PREVIOUS_VERSION_CODE, 1L) }
    }

    @Test
    fun `determine - should return NONE when no previous version but device ID is not created`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false
        )
        val packageInfo = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("1.0.0", 1L),
            previousPackageVersionInfo = null
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device, packageInfo)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
        verify { keyValueRepository.putString(any(), any()) }
        verify { keyValueRepository.putLong(any(), any()) }
    }

    @Test
    fun `determine - should return UPDATE when previous version exists and is different from current`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false
        )
        val packageInfo = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("2.0.0", 2L),
            previousPackageVersionInfo = PackageVersionInfo("1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device, packageInfo)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
        verify { keyValueRepository.putString(PackageInfo.KEY_PREVIOUS_VERSION_NAME, "2.0.0") }
        verify { keyValueRepository.putLong(PackageInfo.KEY_PREVIOUS_VERSION_CODE, 2L) }
    }

    @Test
    fun `determine - should return NONE when previous version exists and is same as current`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false
        )
        val packageInfo = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("1.0.0", 1L),
            previousPackageVersionInfo = PackageVersionInfo("1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device, packageInfo)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
        verify { keyValueRepository.putString(any(), any()) }
        verify { keyValueRepository.putLong(any(), any()) }
    }

    @Test
    fun `determine - should return UPDATE when version name is different but version code is same`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false
        )
        val packageInfo = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("1.0.1", 1L),
            previousPackageVersionInfo = PackageVersionInfo("1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device, packageInfo)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
        verify { keyValueRepository.putString(PackageInfo.KEY_PREVIOUS_VERSION_NAME, "1.0.1") }
        verify { keyValueRepository.putLong(PackageInfo.KEY_PREVIOUS_VERSION_CODE, 1L) }
    }

    @Test
    fun `determine - should return UPDATE when version code is different but version name is same`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false
        )
        val packageInfo = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("1.0.0", 2L),
            previousPackageVersionInfo = PackageVersionInfo("1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device, packageInfo)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
        verify { keyValueRepository.putString(PackageInfo.KEY_PREVIOUS_VERSION_NAME, "1.0.0") }
        verify { keyValueRepository.putLong(PackageInfo.KEY_PREVIOUS_VERSION_CODE, 2L) }
    }

    @Test
    fun `determine - should return NONE and log warning when exception occurs`() {
        // given
        val device = mockk<Device> {
            every { isIdCreated } throws RuntimeException("Test exception")
        }
        val packageInfo = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device, packageInfo)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
        verify(exactly = 0) { keyValueRepository.putString(any(), any()) }
        verify(exactly = 0) { keyValueRepository.putLong(any(), any()) }
    }

    @Test
    fun `saveVersionInfo - should handle exception gracefully when saving version info fails`() {
        // given
        every { keyValueRepository.putString(any(), any()) } throws RuntimeException("Save failed")

        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = true
        )
        val packageInfo = MockPackageInfo(
            currentPackageVersionInfo = PackageVersionInfo("1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device, packageInfo)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
    }
}