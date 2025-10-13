package io.hackle.android.internal.application.install

import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.platform.packageinfo.PackageInfo
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock

internal class ApplicationInstallStateManager(
    private val clock: Clock,
    private val packageInfo: PackageInfo,
    private val keyValueRepository: KeyValueRepository,
    private val applicationInstallDeterminer: ApplicationInstallDeterminer
) : ApplicationListenerRegistry<ApplicationInstallStateListener>() {

    private var previousVersion: PackageVersionInfo? = null

    internal fun initialize() {
        previousVersion = loadPreviousPackageVersion()
    }

    internal fun checkApplicationInstall() {
        val state = applicationInstallDeterminer.determine(packageInfo.packageVersionInfo, previousVersion)
        saveCurrentVersion(packageInfo.packageVersionInfo)
        if (state != ApplicationInstallState.NONE) {
            log.debug { "application($state)" }
            val timestamp = clock.currentMillis()
            when (state) {
                ApplicationInstallState.INSTALL -> publishInstall(packageInfo.packageVersionInfo, timestamp)
                ApplicationInstallState.UPDATE -> publishUpdate(
                    previousVersion,
                    packageInfo.packageVersionInfo,
                    timestamp
                )

                else -> Unit
            }
        }
    }

    private fun publishInstall(versionInfo: PackageVersionInfo, timestamp: Long) {
        listeners.forEach { listener ->
            listener.onInstall(versionInfo, timestamp)
        }
    }

    private fun publishUpdate(
        previousVersion: PackageVersionInfo?,
        currentVersion: PackageVersionInfo,
        timestamp: Long
    ) {
        listeners.forEach { listener ->
            listener.onUpdate(previousVersion, currentVersion, timestamp)
        }
    }

    private fun loadPreviousPackageVersion(): PackageVersionInfo? {
        val previousVersionName = keyValueRepository.getString(KEY_PREVIOUS_VERSION_NAME)
        val previousVersionCode = keyValueRepository.getLong(KEY_PREVIOUS_VERSION_CODE, Long.MIN_VALUE)
            .takeUnless { it == Long.MIN_VALUE }

        return if (previousVersionName != null && previousVersionCode != null) {
            PackageVersionInfo(
                versionName = previousVersionName,
                versionCode = previousVersionCode
            )
        } else {
            null
        }
    }

    private fun saveCurrentVersion(currentVersion: PackageVersionInfo) {
        keyValueRepository.putString(KEY_PREVIOUS_VERSION_NAME, currentVersion.versionName)
        keyValueRepository.putLong(KEY_PREVIOUS_VERSION_CODE, currentVersion.versionCode)
    }

    companion object Companion {
        private val log = Logger<ApplicationInstallStateManager>()

        private const val KEY_PREVIOUS_VERSION_NAME = "previous_version_name"
        private const val KEY_PREVIOUS_VERSION_CODE = "previous_version_code"
    }
}
