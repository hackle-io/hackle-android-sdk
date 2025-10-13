package io.hackle.android.internal.application.install

import io.hackle.android.internal.core.listener.ApplicationListener
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo

internal interface ApplicationInstallStateListener : ApplicationListener {
    fun onInstall(versionInfo: PackageVersionInfo, timestamp: Long)
    fun onUpdate(previousVersion: PackageVersionInfo?, currentVersion: PackageVersionInfo, timestamp: Long)
}
