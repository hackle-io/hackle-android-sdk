package io.hackle.android.internal.bridge.model

import io.hackle.android.internal.bridge.HackleBridgeParameters
import io.hackle.android.internal.utils.json.parseJson

internal class BridgeInvocation(string: String) {

    enum class Command(val text: String) {
        GET_SESSION_ID("getSessionId"),
        GET_USER("getUser"),
        SET_USER("setUser"),
        SET_USER_ID("setUserId"),
        SET_DEVICE_ID("setDeviceId"),
        SET_USER_PROPERTY("setUserProperty"),
        UPDATE_USER_PROPERTY("updateUserProperties"),
        UPDATE_PUSH_SUBSCRIPTIONS("updatePushSubscriptions"),
        UPDATE_SMS_SUBSCRIPTIONS("updateSmsSubscriptions"),
        UPDATE_KAKAO_SUBSCRIPTIONS("updateKakaoSubscriptions"),
        RESET_USER("resetUser"),
        SET_PHONE_NUMBER("setPhoneNumber"),
        UNSET_PHONE_NUMBER("unsetPhoneNumber"),
        VARIATION("variation"),
        VARIATION_DETAIL("variationDetail"),
        IS_FEATURE_ON("isFeatureOn"),
        FEATURE_FLAG_DETAIL("featureFlagDetail"),
        TRACK("track"),
        REMOTE_CONFIG("remoteConfig"),
        SET_CURRENT_SCREEN("setCurrentScreen"),
        SHOW_USER_EXPLORER("showUserExplorer"),
        HIDE_USER_EXPLORER("hideUserExplorer");

        companion object {
            private val ALL = values().associateBy { it.text }
            fun from(name: String): Command {
                return requireNotNull(ALL[name]) { "name[$name]" }
            }
        }
    }

    val command: Command
    val parameters: HackleBridgeParameters

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
        this.parameters = invocation[KEY_PARAMETERS] as? HackleBridgeParameters ?: HashMap()
    }

    companion object {
        private const val KEY_HACKLE = "_hackle"
        private const val KEY_COMMAND = "command"
        private const val KEY_PARAMETERS = "parameters"
    }
}