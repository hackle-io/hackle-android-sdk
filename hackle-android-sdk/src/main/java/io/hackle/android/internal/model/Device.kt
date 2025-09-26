package io.hackle.android.internal.model

import android.content.Context
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.platform.AndroidPlatform
import io.hackle.android.internal.platform.Platform
import io.hackle.android.internal.platform.model.PackageInfo
import java.util.Locale
import java.util.UUID

internal interface Device {

    val id: String
    val isIdCreated: Boolean
    val properties: Map<String, Any>
    val packageInfo: PackageInfo

    companion object {

        private const val ID_KEY = "device_id"
        const val KEY_PREVIOUS_VERSION_NAME = "previous_version_name"
        const val KEY_PREVIOUS_VERSION_CODE = "previous_version_code"

        fun create(context: Context, keyValueRepository: KeyValueRepository): Device {
            var isDeviceIdCreated = false
            val deviceId = keyValueRepository.getString(ID_KEY) {
                isDeviceIdCreated = true
                UUID.randomUUID().toString() 
            }
            val previousVersionName = keyValueRepository.getString(KEY_PREVIOUS_VERSION_NAME)
            val previousVersionCode = keyValueRepository.getLong(KEY_PREVIOUS_VERSION_CODE, Long.MIN_VALUE)
                .takeUnless { it == Long.MIN_VALUE }
            return DeviceImpl(
                id = deviceId,
                isIdCreated = isDeviceIdCreated,
                platform = AndroidPlatform(context, previousVersionName, previousVersionCode),
            )
        }
    }
}

internal data class DeviceImpl(
    override val id: String,
    override val isIdCreated: Boolean,
    private val platform: Platform,
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
                "screenWidth" to deviceInfo.screenInfo.width,
                "screenHeight" to deviceInfo.screenInfo.height,
                "isApp" to true
            )
        }
    
    override val packageInfo: PackageInfo
        get() = platform.getPackageInfo()

    private fun Locale.toLocaleString() = "${this.language}-${this.country}"
}
