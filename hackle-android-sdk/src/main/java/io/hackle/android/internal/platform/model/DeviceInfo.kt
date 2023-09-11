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
) {

    data class ScreenInfo(
        val orientation: Orientation,
        val width: Int,
        val height: Int,
    )

    enum class Orientation(val displayName: String) {
        PORTRAIT("portrait"),
        LANDSCAPE("landscape")
    }

    enum class ConnectionType {
        NONE, WIFI, MOBILE
    }
}