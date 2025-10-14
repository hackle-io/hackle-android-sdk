package io.hackle.android.internal.application.install

import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.platform.PlatformManager
import io.hackle.android.internal.platform.packageinfo.PackageInfo
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock

internal class ApplicationInstallStateManager(
    private val clock: Clock,
    private val platformManager: PlatformManager,
    private val applicationInstallDeterminer: ApplicationInstallDeterminer
) : ApplicationListenerRegistry<ApplicationInstallStateListener>() {
    
    internal fun checkApplicationInstall() {
        val state = applicationInstallDeterminer.determine(platformManager.previousVersion, platformManager.currentVersion, platformManager.isDeviceIdCreated)
        if (state != ApplicationInstallState.NONE) {
            log.debug { "application($state)" }
            val timestamp = clock.currentMillis()
            when (state) {
                ApplicationInstallState.INSTALL -> publishInstall(platformManager.currentVersion, timestamp)
                ApplicationInstallState.UPDATE -> publishUpdate(
                    platformManager.previousVersion,
                    platformManager.currentVersion,
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

    companion object Companion {
        private val log = Logger<ApplicationInstallStateManager>()
    }
}
