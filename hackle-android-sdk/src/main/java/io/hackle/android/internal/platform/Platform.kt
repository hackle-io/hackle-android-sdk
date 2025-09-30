package io.hackle.android.internal.platform

import android.content.Context
import android.os.Build
import io.hackle.android.internal.platform.helper.DeviceHelper
import io.hackle.android.internal.platform.model.DeviceInfo
import java.util.TimeZone

internal interface Platform {
    fun getCurrentDeviceInfo(): DeviceInfo
}

internal class AndroidPlatform(val context: Context) : Platform {
    
    override fun getCurrentDeviceInfo(): DeviceInfo {
        val displayMetrics = DeviceHelper.getDisplayMetrics(context)
        return DeviceInfo(
            osName = "Android",
            osVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            type = DeviceHelper.getDeviceType(context),
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            locale = DeviceHelper.getDeviceLocale(),
            timezone = TimeZone.getDefault(),
            screenInfo = DeviceInfo.ScreenInfo(
                width = displayMetrics.widthPixels,
                height = displayMetrics.heightPixels,
            ),
        )
    }
}