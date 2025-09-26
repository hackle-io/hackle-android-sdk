package io.hackle.android.internal.platform.model

internal data class PackageInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val previousVersionName: String?,
    val previousVersionCode: Long?,
)