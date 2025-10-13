package io.hackle.android.internal.platform.device

import android.content.Context
import android.os.Build
import io.hackle.android.internal.database.repository.KeyValueRepository
import java.util.Locale
import java.util.TimeZone

internal interface Device {

    val id: String
    val properties: Map<String, Any>

    companion object {
        private const val ID_KEY = "device_id"

        fun create(context: Context, deviceId: String): Device {
            val displayMetrics = DeviceHelper.getDisplayMetrics(context)

            return DeviceImpl(
                id = deviceId,
                deviceInfo = DeviceInfo(
                    osName = "Android",
                    osVersion = Build.VERSION.RELEASE,
                    model = Build.MODEL,
                    type = DeviceHelper.getDeviceType(context),
                    brand = Build.BRAND,
                    manufacturer = Build.MANUFACTURER,
                    locale = DeviceHelper.getDeviceLocale(),
                    timezone = TimeZone.getDefault(),
                    screenInfo = DeviceInfo.ScreenInfo(
                        width = displayMetrics.widthPixels,
                        height = displayMetrics.heightPixels
                    )
                )
            )
        }
        
        fun getDeviceId(keyValueRepository: KeyValueRepository, mapping: (String) -> String): String {
            return keyValueRepository.getString(ID_KEY, mapping)
        }
    }
}

internal data class DeviceImpl(
    override val id: String,
    private val deviceInfo: DeviceInfo
) : Device {

    override val properties: Map<String, Any>
        get() {
            return mapOf(
                "platform" to "Android",
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

    private fun Locale.toLocaleString() = "${this.language}-${this.country}"
}
