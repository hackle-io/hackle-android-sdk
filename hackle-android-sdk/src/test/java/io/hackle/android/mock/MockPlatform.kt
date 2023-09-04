package io.hackle.android.mock

import io.hackle.android.internal.platform.Platform
import io.hackle.android.internal.platform.model.DeviceInfo
import io.hackle.android.internal.platform.model.PackageInfo
import java.util.Locale
import java.util.TimeZone

internal class MockPlatform(
    val orientation: DeviceInfo.Orientation = DeviceInfo.Orientation.PORTRAIT,
    val connectionType: DeviceInfo.ConnectionType = DeviceInfo.ConnectionType.MOBILE
) : Platform {

    override fun getPackageInfo(): PackageInfo =
        PackageInfo(
            packageName = "io.hackle.app",
            versionName = "1.1.1",
            versionCode = 10101L
        )

    override fun getCurrentDeviceInfo(): DeviceInfo =
        DeviceInfo(
            osName = "DummyOS",
            osVersion = "1.0.0",
            model = "hackle-123a",
            type = "phone",
            brand = "hackle",
            manufacturer = "hackle manufacture",
            locale = Locale.KOREA,
            timezone = TimeZone.getTimeZone("Asia/Seoul"),
            screenInfo = DeviceInfo.ScreenInfo(
                orientation = orientation,
                density = 440,
                width = 1080,
                height = 1920,
            ),
            connectionType = connectionType,
        )
}