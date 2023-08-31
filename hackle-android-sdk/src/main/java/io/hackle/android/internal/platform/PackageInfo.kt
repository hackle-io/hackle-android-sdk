package io.hackle.android.internal.platform

import android.content.Context
import android.os.Build

internal interface PackageInfo {
    val packageName: String
    val versionName: String
    val versionCode: Long
}

internal class AndroidPackageInfo(val context: Context) : PackageInfo {

    private var _packageName: String = ""
    override val packageName: String
        get() = _packageName

    private var _versionName: String = ""
    override val versionName: String
        get() = _versionName

    private var _versionCode: Long = 0L
    override val versionCode: Long
        get() = _versionCode

    init {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            _packageName = packageInfo.packageName
            _versionName = packageInfo.versionName
            @Suppress("DEPRECATION")
            _versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                packageInfo.longVersionCode else packageInfo.versionCode.toLong()
        } catch (_: Throwable) { }
    }
}