package io.hackle.android.internal.model

import android.content.Context
import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.platform.AndroidPlatform
import io.hackle.android.internal.platform.Platform
import io.hackle.android.internal.platform.model.DeviceInfo
import java.util.UUID

internal interface Device {

    val id: String
    val properties: Map<String, Any>

    companion object {

        const val ID_KEY = "device_id"

        fun create(context: Context, keyValueRepository: KeyValueRepository): Device {
            return DeviceImpl(
                keyValueRepository = keyValueRepository,
                platform = AndroidPlatform(context),
            )
        }
    }
}

internal data class DeviceImpl(
    val keyValueRepository: KeyValueRepository,
    val platform: Platform,
) : Device {

    override val id: String
        get() = keyValueRepository.getString(Device.ID_KEY) { UUID.randomUUID().toString() }

    override val properties: Map<String, Any>
        get() {
            val packageInfo = platform.getPackageInfo()
            val deviceInfo = platform.getCurrentDeviceInfo()
            return mapOf(
                "packageName" to packageInfo.packageName,
                "osName" to deviceInfo.osName,
                "osVersion" to deviceInfo.osVersion,
                "platform" to "Android",
                "versionCode" to packageInfo.versionCode,
                "versionName" to packageInfo.versionName,
                "deviceModel" to deviceInfo.model,
                "deviceType" to deviceInfo.type,
                "deviceBrand" to deviceInfo.brand,
                "deviceManufacturer" to deviceInfo.manufacturer,
                "locale" to deviceInfo.locale.toString(),
                "language" to deviceInfo.locale.language,
                "timeZone" to deviceInfo.timezone.id,
                "orientation" to if (deviceInfo.screenInfo.orientation == DeviceInfo.Orientation.PORTRAIT)
                    "portrait" else "landscape",
                "screenDpi" to deviceInfo.screenInfo.density,
                "screenWidth" to deviceInfo.screenInfo.width,
                "screenHeight" to deviceInfo.screenInfo.height,
                "carrierCode" to deviceInfo.networkInfo.carrier,
                "carrierName" to deviceInfo.networkInfo.carrierName,
                "isWifi" to (deviceInfo.networkInfo.connectionType == DeviceInfo.ConnectionType.WIFI),
                "isApp" to true
            )
        }
}
