package io.hackle.android.internal.platform

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.telephony.TelephonyManager
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

    @Suppress("DEPRECATION")
    override fun getPackageInfo(): PackageInfo {
        var packageName = ""
        var versionName = ""
        var versionCode = 0L
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageName = packageInfo.packageName
            versionName = packageInfo.versionName
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                packageInfo.longVersionCode else packageInfo.versionCode.toLong()
        } catch (_: Throwable) { }
        return PackageInfo(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
        )
    }

    override fun getCurrentDeviceInfo(): DeviceInfo {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
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
            networkInfo = DeviceInfo.NetworkInfo(
                carrier = telephonyManager.networkOperator,
                carrierName = telephonyManager.networkOperatorName,
                connectionType = NetworkHelper.getConnectionType(context),
            ),
        )
    }
}