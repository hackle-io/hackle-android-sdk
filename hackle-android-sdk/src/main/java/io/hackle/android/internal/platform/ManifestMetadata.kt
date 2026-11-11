package io.hackle.android.internal.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import io.hackle.sdk.core.internal.log.Logger

internal object ManifestMetadata {
    private val log = Logger<ManifestMetadata>()

    private fun values(context: Context): Bundle? = try {
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
    } catch (e: Exception) {
        log.debug { "Failed to read application metadata [error=${e.javaClass.simpleName}]" }
        null
    }

    fun boolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        val values = values(context) ?: return defaultValue
        return if (values.containsKey(key)) values.getBoolean(key, defaultValue) else defaultValue
    }

    fun resourceId(context: Context, key: String): Int? {
        val values = values(context) ?: return null
        if (!values.containsKey(key)) return null
        return values.getInt(key).takeIf { it != 0 }
    }
}
