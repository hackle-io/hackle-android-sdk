package io.hackle.android.internal.model

import io.hackle.android.internal.database.MapKeyValueRepository
import io.hackle.android.internal.platform.DeviceInfo
import io.hackle.android.internal.platform.PackageInfo
import io.hackle.android.mock.MockDeviceInfo
import io.hackle.android.mock.MockPackageInfo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.UUID

class DeviceTest {

    @Test
    fun `create device with normal case`() {
        val deviceId = UUID.randomUUID().toString()
        val repository = MapKeyValueRepository()
        repository.putString("device_id", deviceId)
        val packageInfo = MockPackageInfo()
        val deviceInfo = MockDeviceInfo()
        val device = DeviceImpl(
            packageInfo = packageInfo,
            deviceInfo = deviceInfo,
            keyValueRepository = repository,
        )
        assertThat(device.id, `is`(deviceId))
        assertPackageProperties(device.properties, packageInfo)
        assertDeviceProperties(device.properties, deviceInfo)
    }

    @Test
    fun `create device with wifi connection case`() {
        val deviceId = UUID.randomUUID().toString()
        val repository = MapKeyValueRepository()
        repository.putString("device_id", deviceId)
        val packageInfo = MockPackageInfo()
        val deviceInfo = MockDeviceInfo(connectionType = DeviceInfo.ConnectionType.WIFI)
        val device = DeviceImpl(
            packageInfo = packageInfo,
            deviceInfo = deviceInfo,
            keyValueRepository = repository,
        )
        assertThat(device.id, `is`(deviceId))
        assertPackageProperties(device.properties, packageInfo)
        assertDeviceProperties(device.properties, deviceInfo)
    }

    @Test
    fun `create device with landscape orientation case`() {
        val deviceId = UUID.randomUUID().toString()
        val repository = MapKeyValueRepository()
        repository.putString("device_id", deviceId)
        val packageInfo = MockPackageInfo()
        val deviceInfo = MockDeviceInfo(orientation = DeviceInfo.Orientation.LANDSCAPE)
        val device = DeviceImpl(
            packageInfo = packageInfo,
            deviceInfo = deviceInfo,
            keyValueRepository = repository,
        )
        assertThat(device.id, `is`(deviceId))
        assertPackageProperties(device.properties, packageInfo)
        assertDeviceProperties(device.properties, deviceInfo)
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
        val orientation = if (deviceInfo.screenInfo.orientation == DeviceInfo.Orientation.PORTRAIT)
            "portrait" else "landscape"
        assertThat(properties["orientation"], `is`(orientation))
        assertThat(properties["screenDpi"], `is`(deviceInfo.screenInfo.density))
        assertThat(properties["screenWidth"], `is`(deviceInfo.screenInfo.width))
        assertThat(properties["screenHeight"], `is`(deviceInfo.screenInfo.height))

        assertThat(properties["carrierCode"], `is`(deviceInfo.networkInfo.carrier))
        assertThat(properties["carrierName"], `is`(deviceInfo.networkInfo.carrierName))
        assertThat(properties["isWifi"], `is`(deviceInfo.networkInfo.connectionType == DeviceInfo.ConnectionType.WIFI))
        assertThat(properties["isApp"], `is`(true))
    }
}