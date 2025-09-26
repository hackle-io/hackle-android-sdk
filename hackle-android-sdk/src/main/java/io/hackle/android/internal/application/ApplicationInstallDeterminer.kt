package io.hackle.android.internal.application

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.model.Device

internal class ApplicationInstallDeterminer(
    private val keyValueRepository: KeyValueRepository,
    private val device: Device
) {
    fun determine(): ApplicationInstallState {
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
            keyValueRepository.putString(Device.KEY_PREVIOUS_VERSION_NAME, device.packageInfo.versionName)
            keyValueRepository.putLong(Device.KEY_PREVIOUS_VERSION_CODE, device.packageInfo.versionCode)
        }

        return applicationInstallState
    }

    private data class Version(
        val name: String?,
        val code: Long?
    ) {
        val isAvailable: Boolean
            get() = name != null && code != null
    }
}