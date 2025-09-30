package io.hackle.android.internal.application

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.model.PackageInfo
import io.hackle.android.internal.platform.model.PackageVersionInfo
import io.hackle.sdk.core.internal.log.Logger

internal class ApplicationInstallDeterminer(
    private val keyValueRepository: KeyValueRepository,
    private val device: Device,
    private val packageInfo: PackageInfo
) {
    fun determine(): ApplicationInstallState {
        return try {
            val previousVersion = packageInfo.previousPackageVersionInfo
            val currentVersion = packageInfo.currentPackageVersionInfo

            val state = when {
                previousVersion == null && device.isIdCreated -> ApplicationInstallState.INSTALL
                previousVersion != null && previousVersion != currentVersion -> ApplicationInstallState.UPDATE
                else -> ApplicationInstallState.NONE
            }

            saveVersionInfo(currentVersion)

            state
        } catch (e: Exception) {
            log.warn { "Failed to determine application install state, returning NONE: ${e.message}" }
            ApplicationInstallState.NONE
        }
    }

    private fun saveVersionInfo(packageVersionInfo: PackageVersionInfo) {
        keyValueRepository.putString(PackageInfo.KEY_PREVIOUS_VERSION_NAME, packageVersionInfo.versionName)
        keyValueRepository.putLong(PackageInfo.KEY_PREVIOUS_VERSION_CODE, packageVersionInfo.versionCode!!)
    }

    companion object {
        private val log = Logger<ApplicationInstallDeterminer>()
    }
}