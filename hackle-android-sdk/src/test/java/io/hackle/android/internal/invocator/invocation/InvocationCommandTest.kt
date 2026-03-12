package io.hackle.android.internal.invocator.invocation

import org.junit.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.message

class InvocationCommandTest {

    @Test
    fun `command`() {
        val testCases = mapOf(
            "getSessionId" to InvocationCommand.GET_SESSION_ID,
            "getUser" to InvocationCommand.GET_USER,
            "setUser" to InvocationCommand.SET_USER,
            "setUserId" to InvocationCommand.SET_USER_ID,
            "setDeviceId" to InvocationCommand.SET_DEVICE_ID,
            "setUserProperty" to InvocationCommand.SET_USER_PROPERTY,
            "updateUserProperties" to InvocationCommand.UPDATE_USER_PROPERTY,
            "setPhoneNumber" to InvocationCommand.SET_PHONE_NUMBER,
            "unsetPhoneNumber" to InvocationCommand.UNSET_PHONE_NUMBER,
            "resetUser" to InvocationCommand.RESET_USER,
            "updatePushSubscriptions" to InvocationCommand.UPDATE_PUSH_SUBSCRIPTIONS,
            "updateSmsSubscriptions" to InvocationCommand.UPDATE_SMS_SUBSCRIPTIONS,
            "updateKakaoSubscriptions" to InvocationCommand.UPDATE_KAKAO_SUBSCRIPTIONS,
            "variation" to InvocationCommand.VARIATION,
            "variationDetail" to InvocationCommand.VARIATION_DETAIL,
            "isFeatureOn" to InvocationCommand.IS_FEATURE_ON,
            "featureFlagDetail" to InvocationCommand.FEATURE_FLAG_DETAIL,
            "track" to InvocationCommand.TRACK,
            "remoteConfig" to InvocationCommand.REMOTE_CONFIG,
            "setCurrentScreen" to InvocationCommand.SET_CURRENT_SCREEN,
            "showUserExplorer" to InvocationCommand.SHOW_USER_EXPLORER,
            "hideUserExplorer" to InvocationCommand.HIDE_USER_EXPLORER,
            "getCurrentInAppMessageView" to InvocationCommand.GET_CURRENT_IN_APP_MESSAGE_VIEW,
            "closeInAppMessageView" to InvocationCommand.CLOSE_IN_APP_MESSAGE_VIEW,
            "handleInAppMessageView" to InvocationCommand.HANDLE_IN_APP_MESSAGE_VIEW,
        )

        for ((commandString, expected) in testCases) {
            expectThat(InvocationCommand.from(commandString)).isEqualTo(expected)
        }
    }

    @Test
    fun `invalid command`() {
        expectThrows<IllegalArgumentException> {
            InvocationCommand.from("unknownCommand")
        }.message.isEqualTo("Unsupported InvocationCommand [unknownCommand]")
    }

    @Test
    fun `empty command`() {
        expectThrows<IllegalArgumentException> {
            InvocationCommand.from("")
        }
    }
}
