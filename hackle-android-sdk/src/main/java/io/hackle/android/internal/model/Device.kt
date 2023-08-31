package io.hackle.android.internal.model

import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.platform.DeviceInfo
import io.hackle.android.internal.platform.PackageInfo
import java.util.UUID

internal data class Device(
    val id: String,
    val properties: Map<String, Any>,
) {

    companion object {

        private const val ID_KEY = "device_id"

        fun create(packageInfo: PackageInfo, deviceInfo: DeviceInfo, keyValueRepository: KeyValueRepository): Device {
            val deviceId = keyValueRepository.getString(ID_KEY) { UUID.randomUUID().toString() }
            val screenInfo = deviceInfo.screenInfo
            val networkInfo = deviceInfo.networkInfo
            val properties = mapOf(
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
                "orientation" to deviceInfo.screenInfo.orientation,
                "screenDpi" to screenInfo.density,
                "screenWidth" to screenInfo.width,
                "screenHeight" to screenInfo.height,
                "carrierCode" to networkInfo.carrier,
                "carrierName" to networkInfo.carrierName,
                "isWifi" to (networkInfo.connectionType == DeviceInfo.ConnectionType.WIFI),
                "isApp" to true
            )
            return Device(
                id = deviceId,
                properties = properties
            )
        }
    }
}
