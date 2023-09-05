package io.hackle.android.internal.platform

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import io.hackle.android.internal.platform.helper.DeviceHelper
import io.hackle.android.internal.platform.helper.NetworkHelper
import io.hackle.android.internal.platform.model.DeviceInfo
import io.hackle.android.internal.platform.model.PackageInfo
import java.util.TimeZone

internal interface Platform {

    fun getPackageInfo(): PackageInfo

    fun getCurrentDeviceInfo(): DeviceInfo
}

internal class AndroidPlatform(val context: Context) : Platform {

    private val _packageInfo: PackageInfo

    init {
        var packageName = ""
        var versionName = ""
        var versionCode = 0L

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageName = packageInfo.packageName
            versionName = packageInfo.versionName
            @Suppress("DEPRECATION")
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                packageInfo.longVersionCode else packageInfo.versionCode.toLong()
        } catch (_: Throwable) { }
        _packageInfo = PackageInfo(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
        )
    }

    override fun getPackageInfo(): PackageInfo = _packageInfo

    override fun getCurrentDeviceInfo(): DeviceInfo {
        val orientation = if (context.resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT)
            DeviceInfo.Orientation.LANDSCAPE else DeviceInfo.Orientation.PORTRAIT
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
                orientation = orientation,
                density = displayMetrics.densityDpi,
                width = displayMetrics.widthPixels,
                height = displayMetrics.heightPixels,
            ),
            connectionType = NetworkHelper.getConnectionType(context),
        )
    }
}