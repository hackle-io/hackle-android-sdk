package io.hackle.android.internal.model

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import io.hackle.android.HackleApps
import io.hackle.android.internal.utils.computeIfAbsent
import java.util.*

internal data class Device(
    val id: String,
    val properties: Map<String, Any>,
) {

    companion object {

        private const val ID_KEY = "device_id"

        fun create(context: Context): Device {
            val deviceId = context.getSharedPreferences(HackleApps.PREFERENCES_NAME, MODE_PRIVATE)
                .computeIfAbsent(ID_KEY) { UUID.randomUUID().toString() }
            val properties = mapOf(
                "deviceModel" to Build.MODEL,
                "deviceVendor" to Build.MANUFACTURER,
                "language" to Locale.getDefault().language,
                "osName" to "Android",
                "osVersion" to Build.VERSION.RELEASE,
                "platform" to "Mobile",
                "isApp" to true,
                "versionName" to context.versionName,
            )
            return Device(
                id = deviceId,
                properties = properties
            )
        }

        private val Context.versionName: String
            get() =
                try {
                    packageManager.getPackageInfo(packageName, 0).versionName
                } catch (_: Throwable) {
                    "unknown"
                }
    }
}
