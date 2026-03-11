package io.hackle.android.internal.invocator.invocation

internal enum class InvocationCommand(val command: String) {

    // Session
    GET_SESSION_ID("getSessionId"),

    // User
    GET_USER("getUser"),
    SET_USER("setUser"),
    RESET_USER("resetUser"),

    // UserIdentifiers
    SET_USER_ID("setUserId"),
    SET_DEVICE_ID("setDeviceId"),

    // UserProperties
    SET_USER_PROPERTY("setUserProperty"),
    UPDATE_USER_PROPERTY("updateUserProperties"),

    // User - Phone
    SET_PHONE_NUMBER("setPhoneNumber"),
    UNSET_PHONE_NUMBER("unsetPhoneNumber"),

    // User - Subscription
    UPDATE_PUSH_SUBSCRIPTIONS("updatePushSubscriptions"),
    UPDATE_SMS_SUBSCRIPTIONS("updateSmsSubscriptions"),
    UPDATE_KAKAO_SUBSCRIPTIONS("updateKakaoSubscriptions"),

    // AB_TEST
    VARIATION("variation"),
    VARIATION_DETAIL("variationDetail"),

    // FEATURE_FLAG
    IS_FEATURE_ON("isFeatureOn"),
    FEATURE_FLAG_DETAIL("featureFlagDetail"),

    // REMOTE_CONFIG
    REMOTE_CONFIG("remoteConfig"),

    // Event
    TRACK("track"),

    // Screen
    SET_CURRENT_SCREEN("setCurrentScreen"),

    // InAppMessage
    GET_CURRENT_IN_APP_MESSAGE_VIEW("getCurrentInAppMessageView"),
    CLOSE_IN_APP_MESSAGE_VIEW("closeInAppMessageView"),
    HANDLE_IN_APP_MESSAGE_VIEW("handleInAppMessageView"),

    // Configuration
    SET_OPT_OUT_TRACKING("setOptOutTracking"),

    // DevTools
    SHOW_USER_EXPLORER("showUserExplorer"),
    HIDE_USER_EXPLORER("hideUserExplorer"),
    ;

    companion object {

        private val COMMANDS = values().associateBy { it.command }
        fun from(command: String): InvocationCommand {
            return requireNotNull(COMMANDS[command]) { "Unsupported InvocationCommand [$command]" }
        }
    }
}
