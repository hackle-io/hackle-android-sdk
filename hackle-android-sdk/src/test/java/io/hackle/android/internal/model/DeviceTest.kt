package io.hackle.android.internal.model

import io.hackle.android.internal.platform.model.DeviceInfo
import io.hackle.android.internal.platform.model.PackageInfo
import io.hackle.android.mock.MockPlatform
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.UUID

class DeviceTest {

    @Test
    fun `create device with static compare`() {
        val deviceId = UUID.randomUUID().toString()
        val platform = MockPlatform()
        val device = DeviceImpl(
            id = deviceId,
            isIdCreated = false,
            platform = platform,
        )
        assertThat(device.id, `is`(deviceId))
        assertThat(device.properties["platform"], `is`("Android"))

        assertThat(device.properties["packageName"], `is`("io.hackle.app"))
        assertThat(device.properties["versionName"], `is`("1.1.1"))
        assertThat(device.properties["versionCode"], `is`(10101L))

        assertThat(device.properties["osName"], `is`("DummyOS"))
        assertThat(device.properties["osVersion"], `is`("1.0.0"))
        assertThat(device.properties["deviceModel"], `is`("hackle-123a"))
        assertThat(device.properties["deviceType"], `is`("phone"))
        assertThat(device.properties["deviceBrand"], `is`("hackle"))
        assertThat(device.properties["deviceManufacturer"], `is`("hackle manufacture"))
        assertThat(device.properties["locale"], `is`("ko-KR"))
        assertThat(device.properties["language"], `is`("ko"))
        assertThat(device.properties["timeZone"], `is`("Asia/Seoul"))
        assertThat(device.properties["screenWidth"], `is`(1080))
        assertThat(device.properties["screenHeight"], `is`(1920))
        assertThat(device.properties["isApp"], `is`(true))
    }

    @Test
    fun `create device with normal case`() {
        val deviceId = UUID.randomUUID().toString()
        val platform = MockPlatform()
        val device = DeviceImpl(
            id = deviceId,
            isIdCreated = false,
            platform = platform,
        )
        assertThat(device.id, `is`(deviceId))
        assertPackageProperties(device.properties, platform.getPackageInfo())
        assertDeviceProperties(device.properties, platform.getCurrentDeviceInfo())
    }

    private fun assertPackageProperties(properties: Map<String, Any>, packageInfo: PackageInfo) {
        assertThat(properties["packageName"], `is`(packageInfo.packageName))
        assertThat(properties["versionName"], `is`(packageInfo.versionName))
        assertThat(properties["versionCode"], `is`(packageInfo.versionCode))
    }

    private fun assertDeviceProperties(properties: Map<String, Any>, deviceInfo: DeviceInfo) {
        assertThat(properties["platform"], `is`("Android"))
        assertThat(properties["osName"], `is`(deviceInfo.osName))
        assertThat(properties["osVersion"], `is`(deviceInfo.osVersion))
        assertThat(properties["deviceModel"], `is`(deviceInfo.model))
        assertThat(properties["deviceType"], `is`(deviceInfo.type))
        assertThat(properties["deviceBrand"], `is`(deviceInfo.brand))
        assertThat(properties["deviceManufacturer"], `is`(deviceInfo.manufacturer))
        assertThat(properties["locale"], `is`("${deviceInfo.locale.language}-${deviceInfo.locale.country}"))
        assertThat(properties["language"], `is`(deviceInfo.locale.language))
        assertThat(properties["timeZone"], `is`(deviceInfo.timezone.id))
        assertThat(properties["screenWidth"], `is`(deviceInfo.screenInfo.width))
        assertThat(properties["screenHeight"], `is`(deviceInfo.screenInfo.height))
        assertThat(properties["isApp"], `is`(true))
    }

    @Test
    fun `isIdCreated should be true when device ID is newly created`() {
        // given
        val deviceId = UUID.randomUUID().toString()
        val platform = MockPlatform()

        // when
        val device = DeviceImpl(
            id = deviceId,
            isIdCreated = true,
            platform = platform,
        )

        // then
        assertThat(device.isIdCreated, `is`(true))
    }

    @Test
    fun `isIdCreated should be false when device ID already exists`() {
        // given
        val deviceId = UUID.randomUUID().toString()
        val platform = MockPlatform()

        // when
        val device = DeviceImpl(
            id = deviceId,
            isIdCreated = false,
            platform = platform,
        )

        // then
        assertThat(device.isIdCreated, `is`(false))
    }

    @Test
    fun `packageInfo should return correct package information`() {
        // given
        val deviceId = UUID.randomUUID().toString()
        val platform = MockPlatform()
        val device = DeviceImpl(
            id = deviceId,
            isIdCreated = false,
            platform = platform,
        )

        // when
        val packageInfo = device.packageInfo

        // then
        assertThat(packageInfo.packageName, `is`("io.hackle.app"))
        assertThat(packageInfo.versionName, `is`("1.1.1"))
        assertThat(packageInfo.versionCode, `is`(10101L))
    }
}