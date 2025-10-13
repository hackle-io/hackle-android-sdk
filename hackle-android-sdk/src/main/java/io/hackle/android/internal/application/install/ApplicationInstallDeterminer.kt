package io.hackle.android.internal.application.install

import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.sdk.core.internal.log.Logger

internal class ApplicationInstallDeterminer(
    private val isDeviceIdCreated: Boolean,
) {
    fun determine(currentVersion: PackageVersionInfo, previousVersion: PackageVersionInfo?): ApplicationInstallState {
        return try {
            val state = when {
                previousVersion == null && isDeviceIdCreated -> ApplicationInstallState.INSTALL
                previousVersion != null && previousVersion != currentVersion -> ApplicationInstallState.UPDATE
                else -> ApplicationInstallState.NONE
            }

            state
        } catch (e: Exception) {
            log.warn { "Failed to determine application install state, returning NONE: ${e.message}" }
            ApplicationInstallState.NONE
        }
    }

    companion object {
        private val log = Logger<ApplicationInstallDeterminer>()
    }
}
