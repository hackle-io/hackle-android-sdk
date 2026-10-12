package io.hackle.android.internal.model

import io.hackle.android.internal.platform.device.DeviceImpl
import io.hackle.android.internal.platform.device.DeviceInfo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class DeviceTest {

    @Test
    fun `timeZone follows system time zone changes after device creation`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
            val device = DeviceImpl(UUID.randomUUID().toString(), createTestDeviceInfo())
            val originalProperties = device.properties
            assertThat(originalProperties["timeZone"], `is`("Asia/Seoul"))

            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertThat(device.properties["timeZone"], `is`("America/New_York"))
            assertThat(originalProperties["timeZone"], `is`("Asia/Seoul"))

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertThat(device.properties["timeZone"], `is`("UTC"))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `create device with static compare`() {
        val deviceId = UUID.randomUUID().toString()
        val deviceInfo = createTestDeviceInfo()
        val device = DeviceImpl(
            id = deviceId,
            deviceInfo = deviceInfo,
        )
        assertThat(device.id, `is`(deviceId))
        assertThat(device.properties["platform"], `is`("Android"))

        assertThat(device.properties["osName"], `is`("DummyOS"))
        assertThat(device.properties["osVersion"], `is`("1.0.0"))
        assertThat(device.properties["deviceModel"], `is`("hackle-123a"))
        assertThat(device.properties["deviceType"], `is`("phone"))
        assertThat(device.properties["deviceBrand"], `is`("hackle"))
        assertThat(device.properties["deviceManufacturer"], `is`("hackle manufacture"))
        assertThat(device.properties["locale"], `is`("ko-KR"))
        assertThat(device.properties["language"], `is`("ko"))
        assertThat(device.properties["timeZone"], `is`(TimeZone.getDefault().id))
        assertThat(device.properties["screenWidth"], `is`(1080))
        assertThat(device.properties["screenHeight"], `is`(1920))
        assertThat(device.properties["isApp"], `is`(true))
    }

    @Test
    fun `create device with normal case`() {
        val deviceId = UUID.randomUUID().toString()
        val deviceInfo = createTestDeviceInfo()
        val device = DeviceImpl(
            id = deviceId,
            deviceInfo = deviceInfo,
        )
        assertThat(device.id, `is`(deviceId))
        assertDeviceProperties(device.properties, deviceInfo)
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
        assertThat(properties["timeZone"], `is`(TimeZone.getDefault().id))
        assertThat(properties["screenWidth"], `is`(deviceInfo.screenInfo.width))
        assertThat(properties["screenHeight"], `is`(deviceInfo.screenInfo.height))
        assertThat(properties["isApp"], `is`(true))
    }


    private fun createTestDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            osName = "DummyOS",
            osVersion = "1.0.0",
            model = "hackle-123a",
            type = "phone",
            brand = "hackle",
            manufacturer = "hackle manufacture",
            locale = Locale("ko", "KR"),
            screenInfo = DeviceInfo.ScreenInfo(1080, 1920)
        )
    }
}
