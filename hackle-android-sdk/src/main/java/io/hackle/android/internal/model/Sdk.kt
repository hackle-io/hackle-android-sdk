package io.hackle.android.internal.model

import io.hackle.android.BuildConfig
import io.hackle.android.HackleConfig

internal data class Sdk(
    val key: String,
    val name: String,
    val version: String,
) {

    companion object {
        fun of(sdkKey: String, config: HackleConfig): Sdk {
            val wrapperName = config["wrapper_name"]
            val wrapperVersion = config["wrapper_version"]
            return if (wrapperName != null && wrapperVersion != null) {
                Sdk(sdkKey, wrapperName, wrapperVersion)
            } else {
                Sdk(sdkKey, "android-sdk", BuildConfig.VERSION_NAME)
            }
        }
    }
}
