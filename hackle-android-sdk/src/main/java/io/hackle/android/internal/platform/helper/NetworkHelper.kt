package io.hackle.android.internal.platform.helper

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import io.hackle.android.internal.platform.model.DeviceInfo

internal object NetworkHelper {

    fun getConnectionType(context: Context): DeviceInfo.ConnectionType {
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
    fun getActiveNetwork(connectivityManager: ConnectivityManager): Network? {
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
    fun getConnectionTypeByNetworkCapabilities(connectivityManager: ConnectivityManager): DeviceInfo.ConnectionType {
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
    fun getConnectionTypeByActiveNetworkInfo(connectivityManager: ConnectivityManager): DeviceInfo.ConnectionType {
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