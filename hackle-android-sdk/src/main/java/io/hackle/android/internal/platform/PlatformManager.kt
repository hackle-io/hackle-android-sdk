package io.hackle.android.internal.platform

import android.content.Context
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.internal.platform.packageinfo.PackageInfo
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import java.util.UUID

internal class PlatformManager {
    private val keyValueRepository: KeyValueRepository
    private val _previousVersion: PackageVersionInfo?
    private var _isDeviceIdCreated: Boolean = false
    
    val device: Device
    val packageInfo: PackageInfo
    val isDeviceIdCreated: Boolean get() = _isDeviceIdCreated
    val currentVersion: PackageVersionInfo get() = packageInfo.packageVersion
    val previousVersion: PackageVersionInfo? get() = _previousVersion
    
    constructor(context: Context, keyValueRepository: KeyValueRepository) {
        this.keyValueRepository = keyValueRepository
        this.device = Device.create(context, getDeviceId())
        this.packageInfo = PackageInfo.create(context)
        this._previousVersion = loadPackageVersion()
        
        savePackageVersion()
    }
    
    private fun savePackageVersion() {
        keyValueRepository.putString(KEY_PREVIOUS_VERSION_NAME, currentVersion.versionName)
        keyValueRepository.putLong(KEY_PREVIOUS_VERSION_CODE, currentVersion.versionCode)
    }
    
    private fun loadPackageVersion(): PackageVersionInfo? {
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

    private fun getDeviceId(): String {
        return keyValueRepository.getString(DEVICE_ID_KEY) {
            _isDeviceIdCreated = true
            UUID.randomUUID().toString()
        }
    }

    companion object Companion {
        private const val KEY_PREVIOUS_VERSION_NAME = "previous_version_name"
        private const val KEY_PREVIOUS_VERSION_CODE = "previous_version_code"
        private const val DEVICE_ID_KEY = "device_id"
    }
}
