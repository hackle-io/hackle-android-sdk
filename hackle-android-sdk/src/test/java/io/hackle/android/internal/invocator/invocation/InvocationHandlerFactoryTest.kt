package io.hackle.android.internal.invocator.invocation

import io.hackle.android.internal.invocator.invocation.handlers.*
import io.hackle.sdk.core.internal.utils.safe
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA

class InvocationHandlerFactoryTest {

    @Test
    fun `get`() {
        val sut = InvocationHandlerFactory(mockk())
        for (command in InvocationCommand.values()) {
            expectThat(sut.get(command)) {
                when (command) {
                    InvocationCommand.GET_SESSION_ID -> isA<GetSessionIdInvocationHandler>()
                    InvocationCommand.GET_USER -> isA<GetUserInvocationHandler>()
                    InvocationCommand.SET_USER -> isA<SetUserInvocationHandler>()
                    InvocationCommand.SET_USER_ID -> isA<SetUserIdInvocationHandler>()
                    InvocationCommand.SET_DEVICE_ID -> isA<SetDeviceIdInvocationHandler>()
                    InvocationCommand.SET_USER_PROPERTY -> isA<SetUserPropertyInvocationHandler>()
                    InvocationCommand.UPDATE_USER_PROPERTY -> isA<UpdateUserPropertiesInvocationHandler>()
                    InvocationCommand.SET_PHONE_NUMBER -> isA<SetPhoneNumberInvocationHandler>()
                    InvocationCommand.UNSET_PHONE_NUMBER -> isA<UnsetPhoneNumberInvocationHandler>()
                    InvocationCommand.RESET_USER -> isA<ResetUserInvocationHandler>()
                    InvocationCommand.UPDATE_PUSH_SUBSCRIPTIONS -> isA<UpdatePushSubscriptionsInvocationHandler>()
                    InvocationCommand.UPDATE_SMS_SUBSCRIPTIONS -> isA<UpdateSmsSubscriptionsInvocationHandler>()
                    InvocationCommand.UPDATE_KAKAO_SUBSCRIPTIONS -> isA<UpdateKakaoSubscriptionsInvocationHandler>()
                    InvocationCommand.VARIATION -> isA<VariationInvocationHandler>()
                    InvocationCommand.VARIATION_DETAIL -> isA<VariationDetailInvocationHandler>()
                    InvocationCommand.IS_FEATURE_ON -> isA<IsFeatureOnInvocationHandler>()
                    InvocationCommand.FEATURE_FLAG_DETAIL -> isA<FeatureFlagDetailInvocationHandler>()
                    InvocationCommand.TRACK -> isA<TrackInvocationHandler>()
                    InvocationCommand.REMOTE_CONFIG -> isA<RemoteConfigInvocationHandler>()
                    InvocationCommand.SET_CURRENT_SCREEN -> isA<SetCurrentScreenInvocationHandler>()
                    InvocationCommand.SET_OPT_OUT_TRACKING -> isA<SetOptOutTrackingInvocationHandler>()
                    InvocationCommand.IS_OPT_OUT_TRACKING -> isA<IsOptOutTrackingInvocationHandler>()
                    InvocationCommand.SHOW_USER_EXPLORER -> isA<ShowUserExplorerInvocationHandler>()
                    InvocationCommand.HIDE_USER_EXPLORER -> isA<HideUserExplorerInvocationHandler>()
                    InvocationCommand.GET_CURRENT_IN_APP_MESSAGE_VIEW -> isA<GetCurrentInAppMessageViewInvocationHandler>()
                    InvocationCommand.CLOSE_IN_APP_MESSAGE_VIEW -> isA<CloseInAppMessageViewInvocationHandler>()
                    InvocationCommand.HANDLE_IN_APP_MESSAGE_VIEW -> isA<HandleInAppMessageViewInvocationHandler>()
                }.safe
            }
        }
    }
}
