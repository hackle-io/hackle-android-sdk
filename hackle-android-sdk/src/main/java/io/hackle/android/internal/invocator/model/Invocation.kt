package io.hackle.android.internal.invocator.model

import io.hackle.android.internal.invocator.HackleInvokeParameters
import io.hackle.android.internal.utils.json.parseJson

internal class Invocation(string: String) {

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

        companion object Companion {
            private val ALL = values().associateBy { it.text }
            fun from(name: String): Command {
                return requireNotNull(ALL[name]) { "name[$name]" }
            }
        }
    }

    val command: Command
    val parameters: HackleInvokeParameters
    val browserProperties: HackleBrowserProperties

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
        this.parameters = invocation[KEY_PARAMETERS] as? HackleInvokeParameters ?: HashMap()
        @Suppress("UNCHECKED_CAST")
        this.browserProperties = invocation[KEY_BROWSER_PROPERTIES] as? HackleBrowserProperties ?: HashMap()
    }

    companion object Companion {
        private const val KEY_HACKLE = "_hackle"
        private const val KEY_COMMAND = "command"
        private const val KEY_PARAMETERS = "parameters"
        private const val KEY_BROWSER_PROPERTIES = "browserProperties"

        fun isInvocableString(string: String): Boolean {
            val data = try {
                string.parseJson<Map<String, Any>>()
            } catch (_: Throwable) {
                return false
            }

            @Suppress("UNCHECKED_CAST")
            val invocation = data[KEY_HACKLE] as? Map<String, Any> ?: return false

            @Suppress("UNCHECKED_CAST")
            val commandString = invocation[KEY_COMMAND] as? String ?: return false

            return commandString.isNotEmpty()
        }
    }
}

internal typealias HackleBrowserProperties = Map<String, Any>
