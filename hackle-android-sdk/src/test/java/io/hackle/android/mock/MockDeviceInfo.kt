package io.hackle.android.mock

import io.hackle.android.internal.platform.DeviceInfo
import java.util.Locale
import java.util.TimeZone

internal class MockDeviceInfo(
    orientation: DeviceInfo.Orientation = DeviceInfo.Orientation.PORTRAIT,
    connectionType: DeviceInfo.ConnectionType = DeviceInfo.ConnectionType.MOBILE
) : DeviceInfo {

    override val osName: String = "DummyOS"

    override val osVersion: String = "1.0.0"

    override val model: String = "hackle-123a"

    override val type: String = "phone"

    override val brand: String = "hackle"

    override val manufacturer: String = "hackle manufacture"

    override val locale: Locale = Locale.KOREA

    override val timezone: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

    override val screenInfo: DeviceInfo.ScreenInfo =
        DeviceInfo.ScreenInfo(
            orientation = orientation,
            density = 440,
            width = 1080,
            height = 1920,
        )

    override val networkInfo: DeviceInfo.NetworkInfo =
        DeviceInfo.NetworkInfo(
            carrier = "hackle",
            carrierName = "hackle inc.",
            connectionType = connectionType
        )
}
