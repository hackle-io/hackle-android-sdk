package io.hackle.android.internal.bridge.model

import io.hackle.android.internal.utils.parseJson

internal class BridgeInvocation(string: String) {

    companion object {
        private const val KEY_HACKLE = "_hackle"
        private const val KEY_COMMAND = "command"
        private const val KEY_PARAMETERS = "parameters"
    }

    enum class Command(val text: String) {
        GET_SESSION_ID("getSessionId"),
        GET_USER("getUser"),
        SET_USER("setUser"),
        SET_USER_ID("setUserId"),
        SET_DEVICE_ID("setDeviceId"),
        SET_USER_PROPERTY("setUserProperty"),
        UPDATE_USER_PROPERTY("updateUserProperties"),
        RESET_USER("resetUser"),
        VARIATION("variation"),
        VARIATION_DETAIL("variationDetail"),
        IS_FEATURE_ON("isFeatureOn"),
        FEATURE_FLAG_DETAIL("featureFlagDetail"),
        TRACK("track"),
        REMOTE_CONFIG("remoteConfig"),
        SHOW_USER_EXPLORER("showUserExplorer");

        companion object {
            private val ALL = values().associateBy { it.text }
            fun from(name: String): Command {
                return requireNotNull(ALL[name]) { "name[$name]" }
            }
        }
    }

    val command: Command
    val parameters: Map<String, Any>

    init {
        val data = string.parseJson<Map<String, Any>>()
        @Suppress("UNCHECKED_CAST")
        val invocation = data[KEY_HACKLE] as? Map<String, Any>
            ?: throw IllegalArgumentException("'$KEY_HACKLE' key must be provided.")
        val command = invocation[KEY_COMMAND] as? String
            ?: throw IllegalArgumentException("'$KEY_COMMAND' key must be provided")
        try {
            this.command = Command.from(command)
        } catch (_: Throwable) {
            throw IllegalArgumentException("Unsupported command string received.")
        }
        @Suppress("UNCHECKED_CAST")
        this.parameters = invocation[KEY_PARAMETERS] as? Map<String, Any> ?: HashMap()
    }
}