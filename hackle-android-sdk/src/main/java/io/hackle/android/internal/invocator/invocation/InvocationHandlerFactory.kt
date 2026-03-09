package io.hackle.android.internal.invocator.invocation

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.invocation.InvocationCommand.*
import io.hackle.android.internal.invocator.invocation.handlers.*

internal class InvocationHandlerFactory(
    private val core: HackleAppCore,
) {

    private val handlers = HashMap<InvocationCommand, InvocationHandler<*>>()

    init {
        for (command in InvocationCommand.values()) {
            handlers[command] = create(command)
        }
    }

    fun get(command: InvocationCommand): InvocationHandler<*> {
        return requireNotNull(handlers[command]) { "Not found InvocationHandler [$command]" }
    }

    private fun create(command: InvocationCommand): InvocationHandler<*> {
        return when (command) {
            GET_SESSION_ID -> GetSessionIdInvocationHandler(core)
            GET_USER -> GetUserInvocationHandler(core)
            SET_USER -> SetUserInvocationHandler(core)
            SET_USER_ID -> SetUserIdInvocationHandler(core)
            SET_DEVICE_ID -> SetDeviceIdInvocationHandler(core)
            SET_USER_PROPERTY -> SetUserPropertyInvocationHandler(core)
            UPDATE_USER_PROPERTY -> UpdateUserPropertiesInvocationHandler(core)
            SET_PHONE_NUMBER -> SetPhoneNumberInvocationHandler(core)
            UNSET_PHONE_NUMBER -> UnsetPhoneNumberInvocationHandler(core)
            RESET_USER -> ResetUserInvocationHandler(core)
            UPDATE_PUSH_SUBSCRIPTIONS -> UpdatePushSubscriptionsInvocationHandler(core)
            UPDATE_SMS_SUBSCRIPTIONS -> UpdateSmsSubscriptionsInvocationHandler(core)
            UPDATE_KAKAO_SUBSCRIPTIONS -> UpdateKakaoSubscriptionsInvocationHandler(core)
            VARIATION -> VariationInvocationHandler(core)
            VARIATION_DETAIL -> VariationDetailInvocationHandler(core)
            IS_FEATURE_ON -> IsFeatureOnInvocationHandler(core)
            FEATURE_FLAG_DETAIL -> FeatureFlagDetailInvocationHandler(core)
            TRACK -> TrackInvocationHandler(core)
            REMOTE_CONFIG -> RemoteConfigInvocationHandler(core)
            SET_CURRENT_SCREEN -> SetCurrentScreenInvocationHandler(core)
            SET_OPT_OUT_TRACKING -> SetOptOutTrackingInvocationHandler(core)
            SHOW_USER_EXPLORER -> ShowUserExplorerInvocationHandler(core)
            HIDE_USER_EXPLORER -> HideUserExplorerInvocationHandler(core)
        }
    }
}
