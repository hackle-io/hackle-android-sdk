package io.hackle.android.internal.platform.model

import java.util.Locale
import java.util.TimeZone

internal data class DeviceInfo(
    val osName: String,
    val osVersion: String,
    val model: String,
    val type: String,
    val brand: String,
    val manufacturer: String,
    val locale: Locale,
    val timezone: TimeZone,
    val screenInfo: ScreenInfo,
    val networkInfo: NetworkInfo,
) {

    data class ScreenInfo(
        val orientation: Orientation,
        val width: Int,
        val height: Int,
        val density: Int,
    )

    enum class Orientation(val displayName: String) {
        PORTRAIT("portrait"),
        LANDSCAPE("landscape")
    }

    data class NetworkInfo(
        val carrier: String,
        val carrierName: String,
        val connectionType: ConnectionType,
    )

    enum class ConnectionType {
        NONE, WIFI, MOBILE
    }
}