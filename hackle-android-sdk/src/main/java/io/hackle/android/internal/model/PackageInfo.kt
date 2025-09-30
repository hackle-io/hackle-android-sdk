package io.hackle.android.internal.model

import android.content.Context
import android.os.Build
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.platform.model.PackageVersionInfo


internal interface PackageInfo {
    val currentPackageVersionInfo: PackageVersionInfo
    val previousPackageVersionInfo: PackageVersionInfo?
    val properties: Map<String, Any>

    companion object Companion {
        const val KEY_PREVIOUS_VERSION_NAME = "previous_version_name"
        const val KEY_PREVIOUS_VERSION_CODE = "previous_version_code"

        fun create(context: Context, keyValueRepository: KeyValueRepository): PackageInfo {
            var packageName = ""
            var versionName = ""
            var versionCode = 0L

            val previousVersionName = keyValueRepository.getString(KEY_PREVIOUS_VERSION_NAME)
            val previousVersionCode = keyValueRepository.getLong(KEY_PREVIOUS_VERSION_CODE, Long.MIN_VALUE)
                .takeUnless { it == Long.MIN_VALUE }

            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageName = packageInfo.packageName
                versionName = packageInfo.versionName
                @Suppress("DEPRECATION")
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    packageInfo.longVersionCode else packageInfo.versionCode.toLong()
            } catch (_: Throwable) {
            }
            val currentPackageVersionInfo = PackageVersionInfo(
                versionName = versionName,
                versionCode = versionCode
            )
            val previousPackageVersionInfo = if (previousVersionName != null && previousVersionCode != null) {
                PackageVersionInfo(
                    versionName = previousVersionName,
                    versionCode = previousVersionCode
                )
            } else {
                null
            }

            return PackageInfoImpl(
                packageName = packageName,
                currentPackageVersionInfo = currentPackageVersionInfo,
                previousPackageVersionInfo = previousPackageVersionInfo
            )
        }
    }
}

internal data class PackageInfoImpl(
    private val packageName: String,
    override val currentPackageVersionInfo: PackageVersionInfo,
    override val previousPackageVersionInfo: PackageVersionInfo?,
) : PackageInfo {

    override val properties: Map<String, Any>
        get() {
            return mapOf(
                "packageName" to packageName,
                "versionName" to currentPackageVersionInfo.versionName,
                "versionCode" to currentPackageVersionInfo.versionCode,
            )
        }
}

