package io.hackle.android.internal.platform

import android.content.Context
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.internal.platform.packageinfo.PackageInfo
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*

class PlatformManagerTest {

    private lateinit var mockContext: Context
    private lateinit var keyValueRepository: KeyValueRepository
    private lateinit var platformManager: PlatformManager

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        keyValueRepository = MapKeyValueRepository()

        // Mock Device.Companion.create() to avoid Android system dependencies
        mockkObject(Device.Companion)
        every { Device.create(any(), any()) } answers {
            val deviceId = secondArg<String>()
            MockDevice(deviceId, emptyMap())
        }

        // Mock PackageInfo.Companion.create() to avoid Android system dependencies
        mockkObject(PackageInfo.Companion)
        every { PackageInfo.create(any()) } returns MockPackageInfo(
            PackageVersionInfo("1.0.0", 100L),
            emptyMap()
        )
    }

    @After
    fun tearDown() {
        unmockkObject(Device.Companion)
        unmockkObject(PackageInfo.Companion)
    }

    @Test
    fun `should create new device id when not exists`() {
        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        expectThat(platformManager.device.id).isNotBlank()
        expectThat(platformManager.isDeviceIdCreated).isTrue()
        expectThat(keyValueRepository.getString("device_id")).isNotNull().isEqualTo(platformManager.device.id)
    }

    @Test
    fun `should reuse existing device id when already exists`() {
        // given
        val existingDeviceId = "existing-device-id-123"
        keyValueRepository.putString("device_id", existingDeviceId)

        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        expectThat(platformManager.device.id).isEqualTo(existingDeviceId)
        expectThat(platformManager.isDeviceIdCreated).isFalse()
    }

    @Test
    fun `should initialize device with device id`() {
        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        expectThat(platformManager.device) {
            get { id }.isNotBlank()
        }
    }

    @Test
    fun `should initialize packageInfo from context`() {
        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        expectThat(platformManager.packageInfo).isNotEqualTo(null)
        expectThat(platformManager.currentVersion).isNotEqualTo(null)
    }

    @Test
    fun `should return null for previousVersion when no previous version exists`() {
        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        expectThat(platformManager.previousVersion).isNull()
    }

    @Test
    fun `should load previousVersion when exists in repository`() {
        // given
        keyValueRepository.putString("previous_version_name", "0.9.0")
        keyValueRepository.putLong("previous_version_code", 90L)

        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        expectThat(platformManager.previousVersion).isNotNull().and {
            get { versionName }.isEqualTo("0.9.0")
            get { versionCode }.isEqualTo(90L)
        }
    }

    @Test
    fun `should save currentVersion as previousVersion on initialization`() {
        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        val savedVersionName = keyValueRepository.getString("previous_version_name")
        val savedVersionCode = keyValueRepository.getLong("previous_version_code", Long.MIN_VALUE)

        expectThat(savedVersionName).isNotNull().isEqualTo(platformManager.currentVersion.versionName)
        expectThat(savedVersionCode).isNotEqualTo(Long.MIN_VALUE).isEqualTo(platformManager.currentVersion.versionCode)
    }

    @Test
    fun `should return null for previousVersion when only version name exists`() {
        // given
        keyValueRepository.putString("previous_version_name", "1.0.0")

        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        expectThat(platformManager.previousVersion).isNull()
    }

    @Test
    fun `should return null for previousVersion when only version code exists`() {
        // given
        keyValueRepository.putLong("previous_version_code", 100L)

        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then
        expectThat(platformManager.previousVersion).isNull()
    }

    @Test
    fun `should update previousVersion when app version changes`() {
        // given - first initialization with version 0.9.0
        keyValueRepository.putString("previous_version_name", "0.9.0")
        keyValueRepository.putLong("previous_version_code", 90L)

        // when - create platform manager (simulating app update to new version)
        platformManager = PlatformManager(mockContext, keyValueRepository)

        // then - previous version should be loaded from repository
        expectThat(platformManager.previousVersion).isNotNull().and {
            get { versionName }.isEqualTo("0.9.0")
            get { versionCode }.isEqualTo(90L)
        }

        // and current version (1.0.0) should be saved as new previous version
        val savedVersionName = keyValueRepository.getString("previous_version_name")
        val savedVersionCode = keyValueRepository.getLong("previous_version_code", Long.MIN_VALUE)
        expectThat(savedVersionName).isEqualTo(platformManager.currentVersion.versionName)
        expectThat(savedVersionCode).isEqualTo(platformManager.currentVersion.versionCode)
    }

    @Test
    fun `device id should be consistent across multiple reads`() {
        // when
        platformManager = PlatformManager(mockContext, keyValueRepository)
        val deviceId1 = platformManager.device.id
        val deviceId2 = platformManager.device.id

        // then
        expectThat(deviceId1).isEqualTo(deviceId2)
    }
}
