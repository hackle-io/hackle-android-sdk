package io.hackle.android.internal.platform

import android.Manifest
import android.annotation.TargetApi
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import java.util.Locale
import java.util.TimeZone

interface DeviceInfo {

    data class ScreenInfo(
        val orientation: Int,
        val width: Int,
        val height: Int,
        val density: Int,
    )

    data class NetworkInfo(
        val carrier: String,
        val carrierName: String,
        val connectionType: ConnectionType,
    )

    enum class ConnectionType {
        NONE, WIFI, MOBILE,
    }

    val osName: String
    val osVersion: String
    val model: String
    val type: String
    val brand: String
    val manufacturer: String
    val locale: Locale
    val timezone: TimeZone
    val screenInfo: ScreenInfo
    val networkInfo: NetworkInfo
}

class AndroidDeviceInfo(val context: Context) : DeviceInfo {

    override val osName: String = "Android"

    override val osVersion: String = Build.VERSION.RELEASE

    override val model: String = Build.MODEL

    override val type: String
        get() {
            val service = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            return when (service.currentModeType) {
                Configuration.UI_MODE_TYPE_NORMAL ->
                    if (context.resources.configuration.smallestScreenWidthDp < 600)
                        "phone" else "tablet"
                Configuration.UI_MODE_TYPE_TELEVISION -> "tv"
                Configuration.UI_MODE_TYPE_DESK -> "desktop"
                Configuration.UI_MODE_TYPE_CAR -> "car"
                Configuration.UI_MODE_TYPE_APPLIANCE -> "appliance"
                Configuration.UI_MODE_TYPE_WATCH -> "watch"
                Configuration.UI_MODE_TYPE_VR_HEADSET -> "vr"
                else -> "undefined"
            }
        }

    override val brand: String = Build.BRAND

    override val manufacturer: String = Build.MANUFACTURER

    @Suppress("DEPRECATION")
    override val locale: Locale
        get() {
            val configuration = Resources.getSystem().configuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return if (configuration.locales.isEmpty)
                    Locale.getDefault() else configuration.locales[0]
            } else {
                return configuration.locale
            }
        }

    override val timezone: TimeZone = TimeZone.getDefault()

    @Suppress("DEPRECATION")
    override val screenInfo: DeviceInfo.ScreenInfo
        get() {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = windowManager.currentWindowMetrics
                return DeviceInfo.ScreenInfo(
                    orientation = context.resources.configuration.orientation,
                    density = context.resources.displayMetrics.densityDpi,
                    width = metrics.bounds.width(),
                    height = metrics.bounds.height(),
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
                return DeviceInfo.ScreenInfo(
                    orientation = context.resources.configuration.orientation,
                    density = displayMetrics.densityDpi,
                    width = displayMetrics.widthPixels,
                    height = displayMetrics.heightPixels
                )
            } else {
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                try {
                    val width = Display::class.java
                        .getMethod("getRawWidth")
                        .invoke(windowManager.defaultDisplay) as Int
                    val height = Display::class.java
                        .getMethod("getRawHeight")
                        .invoke(windowManager.defaultDisplay) as Int
                    return DeviceInfo.ScreenInfo(
                        orientation = context.resources.configuration.orientation,
                        density = displayMetrics.densityDpi,
                        width = width,
                        height = height,
                    )
                } catch (_: Throwable) {
                    return DeviceInfo.ScreenInfo(
                        orientation = context.resources.configuration.orientation,
                        density = displayMetrics.densityDpi,
                        width = displayMetrics.widthPixels,
                        height = displayMetrics.heightPixels,
                    )
                }
            }
        }

    override val networkInfo: DeviceInfo.NetworkInfo
        get() {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return DeviceInfo.NetworkInfo(
                carrier = telephonyManager.networkOperator,
                carrierName = telephonyManager.networkOperatorName,
                connectionType = getConnectionType(),
            )
        }

    private fun getConnectionType(): DeviceInfo.ConnectionType {
        if (context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_DENIED) {
            return DeviceInfo.ConnectionType.NONE
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val connectionType = getConnectionTypeByNetworkCapabilities(connectivityManager)
            return if (connectionType != DeviceInfo.ConnectionType.NONE)
                connectionType else getConnectionTypeByActiveNetworkInfo(connectivityManager)
        } else {
            return getConnectionTypeByActiveNetworkInfo(connectivityManager)
        }
    }

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getActiveNetwork(connectivityManager: ConnectivityManager): Network? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return connectivityManager.activeNetwork
        } else {
            val connected: MutableMap<Int, Network> = HashMap()
            for (network in connectivityManager.allNetworks) {
                val info = connectivityManager.getNetworkInfo(network) ?: continue
                if (info.isConnected) { connected[info.type] = network }
            }
            return when {
                connected.containsKey(ConnectivityManager.TYPE_WIFI) -> connected[ConnectivityManager.TYPE_WIFI]
                connected.containsKey(ConnectivityManager.TYPE_MOBILE) -> connected[ConnectivityManager.TYPE_MOBILE]
                else -> null
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getConnectionTypeByNetworkCapabilities(connectivityManager: ConnectivityManager): DeviceInfo.ConnectionType {
        try {
            val activeNetwork = getActiveNetwork(connectivityManager) ?: return DeviceInfo.ConnectionType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return DeviceInfo.ConnectionType.NONE
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> DeviceInfo.ConnectionType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> DeviceInfo.ConnectionType.WIFI
                else -> DeviceInfo.ConnectionType.NONE
            }
        } catch(_: Throwable) {
            return DeviceInfo.ConnectionType.NONE
        }
    }

    @Suppress("DEPRECATION")
    private fun getConnectionTypeByActiveNetworkInfo(connectivityManager: ConnectivityManager): DeviceInfo.ConnectionType {
        try {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return DeviceInfo.ConnectionType.NONE
            return when (networkInfo.type) {
                ConnectivityManager.TYPE_MOBILE -> DeviceInfo.ConnectionType.MOBILE
                ConnectivityManager.TYPE_WIFI -> DeviceInfo.ConnectionType.WIFI
                else -> DeviceInfo.ConnectionType.NONE
            }
        } catch (_: Throwable) {
            return DeviceInfo.ConnectionType.NONE
        }
    }
}