package io.hackle.android.internal.application

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.model.Device
import io.hackle.sdk.core.internal.log.Logger

internal class ApplicationInstallDeterminer(
    private val keyValueRepository: KeyValueRepository,
    private val device: Device
) {
    fun determine(): ApplicationInstallState {
        return try {
            val currentVersion = Version(
                device.packageInfo.versionName,
                device.packageInfo.versionCode
            )
            val previousVersion = Version(
                device.packageInfo.previousVersionName,
                device.packageInfo.previousVersionCode
            )

            val applicationInstallState = when {
                !previousVersion.isAvailable && device.isIdCreated -> ApplicationInstallState.INSTALL
                previousVersion.isAvailable && currentVersion != previousVersion -> ApplicationInstallState.UPDATE
                else -> ApplicationInstallState.NONE
            }

            if (applicationInstallState != ApplicationInstallState.NONE) {
                saveVersionInfo(currentVersion)
            }

            applicationInstallState
        } catch (e: Exception) {
            log.warn { "Failed to determine application install state, returning NONE: ${e.message}" }
            ApplicationInstallState.NONE
        }
    }

    private fun saveVersionInfo(version: Version) {
        try {
            keyValueRepository.putString(Device.KEY_PREVIOUS_VERSION_NAME, version.name ?: "unknown")
            keyValueRepository.putLong(Device.KEY_PREVIOUS_VERSION_CODE, version.code ?: 0L)
        } catch (e: Exception) {
            log.warn { "Failed to save version information: ${e.message}" }
        }
    }

    private data class Version(
        val name: String?,
        val code: Long?
    ) {
        val isAvailable: Boolean
            get() = name != null && code != null
    }

    companion object {
        private val log = Logger<ApplicationInstallDeterminer>()
    }
}