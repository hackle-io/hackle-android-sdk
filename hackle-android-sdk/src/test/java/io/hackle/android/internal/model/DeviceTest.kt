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
    fun `create device with normal case`() {
        val deviceId = UUID.randomUUID().toString()
        val platform = MockPlatform()
        val device = DeviceImpl(
            id = deviceId,
            platform = platform,
        )
        assertThat(device.id, `is`(deviceId))
        assertPackageProperties(device.properties, platform.getPackageInfo())
        assertDeviceProperties(device.properties, platform.getCurrentDeviceInfo())
    }

    @Test
    fun `create device with wifi connection case`() {
        val deviceId = UUID.randomUUID().toString()
        val platform = MockPlatform(connectionType = DeviceInfo.ConnectionType.WIFI)
        val device = DeviceImpl(
            id = deviceId,
            platform = platform,
        )
        assertThat(device.id, `is`(deviceId))
        assertPackageProperties(device.properties, platform.getPackageInfo())
        assertDeviceProperties(device.properties, platform.getCurrentDeviceInfo())
    }

    @Test
    fun `create device with landscape orientation case`() {
        val deviceId = UUID.randomUUID().toString()
        val platform = MockPlatform(orientation = DeviceInfo.Orientation.LANDSCAPE)
        val device = DeviceImpl(
            id = deviceId,
            platform = platform,
        )
        assertThat(device.id, `is`(deviceId))
        assertPackageProperties(device.properties, platform.getPackageInfo())
        assertDeviceProperties(device.properties, platform.getCurrentDeviceInfo())
    }

    private fun assertPackageProperties(properties: Map<String, Any>, packageInfo: PackageInfo) {
        assertThat(properties["packageName"], `is`(packageInfo.packageName))
        assertThat(properties["versionCode"], `is`(packageInfo.versionCode))
        assertThat(properties["versionName"], `is`(packageInfo.versionName))
    }

    private fun assertDeviceProperties(properties: Map<String, Any>, deviceInfo: DeviceInfo) {
        assertThat(properties["osName"], `is`(deviceInfo.osName))
        assertThat(properties["osVersion"], `is`(deviceInfo.osVersion))
        assertThat(properties["platform"], `is`("Android"))

        assertThat(properties["deviceModel"], `is`(deviceInfo.model))
        assertThat(properties["deviceType"], `is`(deviceInfo.type))
        assertThat(properties["deviceBrand"], `is`(deviceInfo.brand))
        assertThat(properties["deviceManufacturer"], `is`(deviceInfo.manufacturer))
        assertThat(properties["locale"], `is`(deviceInfo.locale.toString()))
        assertThat(properties["language"], `is`(deviceInfo.locale.language))
        assertThat(properties["timeZone"], `is`(deviceInfo.timezone.id))
        assertThat(properties["orientation"], `is`(deviceInfo.screenInfo.orientation.displayName))
        assertThat(properties["screenDpi"], `is`(deviceInfo.screenInfo.density))
        assertThat(properties["screenWidth"], `is`(deviceInfo.screenInfo.width))
        assertThat(properties["screenHeight"], `is`(deviceInfo.screenInfo.height))
        assertThat(properties["isWifi"], `is`(deviceInfo.connectionType == DeviceInfo.ConnectionType.WIFI))
        assertThat(properties["isApp"], `is`(true))
    }
}