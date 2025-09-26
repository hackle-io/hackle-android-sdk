package io.hackle.android.internal.application

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.platform.model.PackageInfo
import io.hackle.android.mock.MockDevice
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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
            isIdCreated = true,
            packageInfo = PackageInfo("test.app", "1.0.0", 1L, null, null)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.INSTALL)
        verify { keyValueRepository.putString(Device.KEY_PREVIOUS_VERSION_NAME, "1.0.0") }
        verify { keyValueRepository.putLong(Device.KEY_PREVIOUS_VERSION_CODE, 1L) }
    }

    @Test
    fun `determine - should return NONE when no previous version but device ID is not created`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false,
            packageInfo = PackageInfo("test.app", "1.0.0", 1L, null, null)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

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
            isIdCreated = false,
            packageInfo = PackageInfo("test.app", "2.0.0", 2L, "1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
        verify { keyValueRepository.putString(Device.KEY_PREVIOUS_VERSION_NAME, "2.0.0") }
        verify { keyValueRepository.putLong(Device.KEY_PREVIOUS_VERSION_CODE, 2L) }
    }

    @Test
    fun `determine - should return NONE when previous version exists and is same as current`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false,
            packageInfo = PackageInfo("test.app", "1.0.0", 1L, "1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

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
            isIdCreated = false,
            packageInfo = PackageInfo("test.app", "1.0.1", 1L, "1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
        verify { keyValueRepository.putString(Device.KEY_PREVIOUS_VERSION_NAME, "1.0.1") }
        verify { keyValueRepository.putLong(Device.KEY_PREVIOUS_VERSION_CODE, 1L) }
    }

    @Test
    fun `determine - should return UPDATE when version code is different but version name is same`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false,
            packageInfo = PackageInfo("test.app", "1.0.0", 2L, "1.0.0", 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.UPDATE)
        verify { keyValueRepository.putString(Device.KEY_PREVIOUS_VERSION_NAME, "1.0.0") }
        verify { keyValueRepository.putLong(Device.KEY_PREVIOUS_VERSION_CODE, 2L) }
    }

    @Test
    fun `determine - should return NONE when previous version has null name but current has name`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false,
            packageInfo = PackageInfo("test.app", "1.0.0", 1L, null, 1L)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
    }

    @Test
    fun `determine - should return NONE when previous version has null code but current has code`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = false,
            packageInfo = PackageInfo("test.app", "1.0.0", 1L, "1.0.0", null)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
    }

    @Test
    fun `determine - should return NONE and log warning when exception occurs`() {
        // given
        val device = mockk<Device> {
            every { packageInfo } throws RuntimeException("Test exception")
        }
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

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
        every { keyValueRepository.putLong(any(), any()) } throws RuntimeException("Save failed")

        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = true,
            packageInfo = PackageInfo("test.app", "1.0.0", 1L, null, null)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.NONE)
    }

    @Test
    fun `saveVersionInfo - should save unknown when version name is null`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = true,
            packageInfo = PackageInfo("test.app", "unknown", 1L, null, null)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.INSTALL)
        verify { keyValueRepository.putString(Device.KEY_PREVIOUS_VERSION_NAME, "unknown") }
        verify { keyValueRepository.putLong(Device.KEY_PREVIOUS_VERSION_CODE, 1L) }
    }

    @Test
    fun `saveVersionInfo - should save 0 when version code is null`() {
        // given
        val device = MockDevice(
            id = "test-id",
            properties = emptyMap(),
            isIdCreated = true,
            packageInfo = PackageInfo("test.app", "1.0.0", 0L, null, null)
        )
        val determiner = ApplicationInstallDeterminer(keyValueRepository, device)

        // when
        val result = determiner.determine()

        // then
        expectThat(result).isEqualTo(ApplicationInstallState.INSTALL)
        verify { keyValueRepository.putString(Device.KEY_PREVIOUS_VERSION_NAME, "1.0.0") }
        verify { keyValueRepository.putLong(Device.KEY_PREVIOUS_VERSION_CODE, 0L) }
    }

}