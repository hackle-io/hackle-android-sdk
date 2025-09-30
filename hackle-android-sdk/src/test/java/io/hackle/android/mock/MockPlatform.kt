package io.hackle.android.mock

import io.hackle.android.internal.platform.Platform
import io.hackle.android.internal.platform.model.DeviceInfo
import java.util.Locale
import java.util.TimeZone

internal class MockPlatform() : Platform {

    override fun getCurrentDeviceInfo(): DeviceInfo =
        DeviceInfo(
            osName = "DummyOS",
            osVersion = "1.0.0",
            model = "hackle-123a",
            type = "phone",
            brand = "hackle",
            manufacturer = "hackle manufacture",
            locale = Locale("ko", "KR"),
            timezone = TimeZone.getTimeZone("Asia/Seoul"),
            screenInfo = DeviceInfo.ScreenInfo(1080, 1920)
        )
}