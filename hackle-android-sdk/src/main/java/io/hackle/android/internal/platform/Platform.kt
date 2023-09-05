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

    private var _packageInfo: PackageInfo? = null

    @Suppress("DEPRECATION")
    override fun getPackageInfo(): PackageInfo {
        if (_packageInfo != null) {
            return _packageInfo!!
        }

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val packageName = packageInfo.packageName
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                packageInfo.longVersionCode else packageInfo.versionCode.toLong()
            _packageInfo = PackageInfo(
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
            )
            return _packageInfo!!
        } catch (_: Throwable) {
            return PackageInfo(
                packageName = "",
                versionName = "",
                versionCode = 0L
            )
        }
    }

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