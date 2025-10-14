package io.hackle.android.internal.platform.packageinfo

import android.content.Context
import android.os.Build


internal interface PackageInfo {
    val packageVersion: PackageVersionInfo
    val properties: Map<String, Any>

    companion object Companion {
        
        fun create(context: Context): PackageInfo {
            var packageName = ""
            var versionName = ""
            var versionCode = 0L
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageName = packageInfo.packageName
                versionName = packageInfo.versionName
                @Suppress("DEPRECATION")
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    packageInfo.longVersionCode else packageInfo.versionCode.toLong()
            } catch (_: Throwable) {
            }

            val packageVersionInfo = PackageVersionInfo(
                versionName = versionName,
                versionCode = versionCode
            )
            
            return PackageInfoImpl(packageName, packageVersionInfo)
        }
    }
}

internal data class PackageInfoImpl(
    private val packageName: String,
    override val packageVersion: PackageVersionInfo
) : PackageInfo {

    override val properties: Map<String, Any>
        get() {
            return mapOf(
                "packageName" to packageName,
                "versionName" to packageVersion.versionName,
                "versionCode" to packageVersion.versionCode,
            )
        }
}
