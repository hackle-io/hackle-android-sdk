package io.hackle.android.internal.model

import android.content.Context
import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.platform.AndroidPlatform
import io.hackle.android.internal.platform.Platform
import io.hackle.android.internal.platform.model.DeviceInfo
import java.util.Locale
import java.util.UUID

internal interface Device {

    val id: String
    val properties: Map<String, Any>

    companion object {

        private const val ID_KEY = "device_id"

        fun create(context: Context, keyValueRepository: KeyValueRepository): Device {
            val deviceId = keyValueRepository.getString(ID_KEY) { UUID.randomUUID().toString() }
            return DeviceImpl(
                id = deviceId,
                platform = AndroidPlatform(context),
            )
        }
    }
}

internal data class DeviceImpl(
    override val id: String,
    val platform: Platform,
) : Device {

    override val properties: Map<String, Any>
        get() {
            val packageInfo = platform.getPackageInfo()
            val deviceInfo = platform.getCurrentDeviceInfo()
            return mapOf(
                "platform" to "Android",
                "packageName" to packageInfo.packageName,
                "versionName" to packageInfo.versionName,
                "versionCode" to packageInfo.versionCode,
                "osName" to deviceInfo.osName,
                "osVersion" to deviceInfo.osVersion,
                "deviceModel" to deviceInfo.model,
                "deviceType" to deviceInfo.type,
                "deviceBrand" to deviceInfo.brand,
                "deviceManufacturer" to deviceInfo.manufacturer,
                "locale" to deviceInfo.locale.toLocaleString(),
                "language" to deviceInfo.locale.language,
                "timeZone" to deviceInfo.timezone.id,
                "orientation" to deviceInfo.screenInfo.orientation.displayName,
                "screenWidth" to deviceInfo.screenInfo.width,
                "screenHeight" to deviceInfo.screenInfo.height,
                "isWifi" to deviceInfo.connectionType.isWifi(),
                "isApp" to true
            )
        }

    private fun Locale.toLocaleString() = "${this.language}-${this.country}"

    private fun DeviceInfo.ConnectionType.isWifi() =
        this == DeviceInfo.ConnectionType.WIFI
}
