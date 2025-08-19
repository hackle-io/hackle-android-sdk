package io.hackle.android.internal.invocator

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.model.*
import io.hackle.android.internal.invocator.model.Invocation.Command.*
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.ui.explorer.HackleUserExplorer
import io.hackle.sdk.common.*
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations

internal class HackleInvocatorImpl(
    private val hackleAppCore: HackleAppCore
) : HackleInvocator {
    
    override fun isInvocableString(string: String): Boolean {
        return Invocation.isInvocableString(string)
    }

    override fun invoke(string: String): String {
        val response: InvokeResponse = try {
            val invocation = Invocation(string)
            invoke(invocation.command, invocation.parameters, invocation.browserProperties)
        } catch (throwable: Throwable) {
            InvokeResponse.error(throwable)
        }
        return response.toJsonString()
    }

    private fun invoke(
        command: Invocation.Command,
        parameters: HackleInvokeParameters,
        browserProperties: HackleBrowserProperties
    ): InvokeResponse {
        val hackleAppContext = HackleAppContext.create(browserProperties)
        return when (command) {
            GET_SESSION_ID -> {
                InvokeResponse.success(hackleAppCore.sessionId)
            }

            GET_USER -> {
                val data = hackleAppCore.user.toDto()
                InvokeResponse.success(data)
            }

            SET_USER -> {
                setUser(parameters)
                InvokeResponse.success()
            }

            SET_USER_ID -> {
                setUserId(parameters)
                InvokeResponse.success()
            }

            SET_DEVICE_ID -> {
                setDeviceId(parameters)
                InvokeResponse.success()
            }

            SET_USER_PROPERTY -> {
                setUserProperty(parameters, hackleAppContext)
                InvokeResponse.success()
            }

            UPDATE_USER_PROPERTY -> {
                updateUserProperties(parameters, hackleAppContext)
                InvokeResponse.success()
            }

            UPDATE_PUSH_SUBSCRIPTIONS -> {
                updatePushSubscriptions(parameters, hackleAppContext)
                InvokeResponse.success()
            }

            UPDATE_SMS_SUBSCRIPTIONS -> {
                updateSmsSubscriptions(parameters, hackleAppContext)
                InvokeResponse.success()
            }

            UPDATE_KAKAO_SUBSCRIPTIONS -> {
                updateKakaoSubscriptions(parameters, hackleAppContext)
                InvokeResponse.success()
            }

            RESET_USER -> {
                hackleAppCore.resetUser(hackleAppContext, null)
                InvokeResponse.success()
            }

            SET_PHONE_NUMBER -> {
                setPhoneNumber(parameters, hackleAppContext)
                InvokeResponse.success()
            }

            UNSET_PHONE_NUMBER -> {
                hackleAppCore.unsetPhoneNumber(hackleAppContext, null)
                InvokeResponse.success()
            }

            VARIATION -> {
                val data = variation(parameters, hackleAppContext)
                InvokeResponse.success(data)
            }

            VARIATION_DETAIL -> {
                val data = variationDetail(parameters, hackleAppContext)
                InvokeResponse.success(data)
            }

            IS_FEATURE_ON -> {
                val data = isFeatureOn(parameters, hackleAppContext)
                InvokeResponse.success(data)
            }

            FEATURE_FLAG_DETAIL -> {
                val data = featureFlagDetail(parameters, hackleAppContext)
                InvokeResponse.success(data)
            }

            TRACK -> {
                track(parameters, hackleAppContext)
                InvokeResponse.success()
            }

            REMOTE_CONFIG -> {
                val data = remoteConfig(parameters, hackleAppContext)
                InvokeResponse.success(data)
            }

            SET_CURRENT_SCREEN -> {
                setCurrentScreen(parameters)
                InvokeResponse.success()
            }

            SHOW_USER_EXPLORER -> {
                hackleAppCore.showUserExplorer()
                InvokeResponse.success()
            }

            HIDE_USER_EXPLORER -> {
                hackleAppCore.hideUserExplorer()
                InvokeResponse.success()
            }
        }
    }

    private fun setUser(parameters: HackleInvokeParameters) {
        @Suppress("UNCHECKED_CAST")
        val data = checkNotNull(parameters.userAsMap())
        val dto = UserDto.from(data)
        val user = User.from(dto)
        hackleAppCore.setUser(user, null)
    }

    private fun setUserId(parameters: HackleInvokeParameters) {
        check(parameters.containsKey("userId"))
        val userId = parameters.userId()
        hackleAppCore.setUserId(userId, null)
    }

    private fun setDeviceId(parameters: HackleInvokeParameters) {
        val deviceId = checkNotNull(parameters.deviceId())
        hackleAppCore.setDeviceId(deviceId, null)
    }

    private fun setUserProperty(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext) {
        val key = checkNotNull(parameters.key())
        val value = parameters.value()
        val operations = PropertyOperations.builder()
            .set(key, value)
            .build()
        hackleAppCore.updateUserProperties(operations, hackleAppContext, null)
    }

    private fun updateUserProperties(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.propertyOperationDto())
        val operations = PropertyOperations.from(dto)
        hackleAppCore.updateUserProperties(operations, hackleAppContext, null)
    }

    private fun updatePushSubscriptions(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        hackleAppCore.updatePushSubscriptions(operations, hackleAppContext)
    }

    private fun updateSmsSubscriptions(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        hackleAppCore.updateSmsSubscriptions(operations, hackleAppContext)
    }

    private fun updateKakaoSubscriptions(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        hackleAppCore.updateKakaoSubscriptions(operations, hackleAppContext)
    }

    private fun setPhoneNumber(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext) {
        val phoneNumber = checkNotNull(parameters.phoneNumber())
        hackleAppCore.setPhoneNumber(phoneNumber, hackleAppContext, null)
    }

    private fun variation(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext): String {
        val experimentKey = checkNotNull(parameters.experimentKey())
        val defaultVariationKey = parameters.defaultVariation()
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)

        return hackleAppCore.variationDetail(
            experimentKey,
            parameters.user(),
            defaultVariation,
            hackleAppContext
        ).variation.name
    }

    private fun variationDetail(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext): DecisionDto {
        val experimentKey = checkNotNull(parameters.experimentKey())
        val defaultVariationKey = parameters.defaultVariation()
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)

        return hackleAppCore.variationDetail(
            experimentKey, 
            parameters.user(), 
            defaultVariation, 
            hackleAppContext
        ) .toDto()
    }

    private fun isFeatureOn(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext): Boolean {
        val featureKey = checkNotNull(parameters.featureKey())

        return hackleAppCore.featureFlagDetail(featureKey, parameters.user(), hackleAppContext).isOn
    }

    private fun featureFlagDetail(
        parameters: HackleInvokeParameters,
        hackleAppContext: HackleAppContext
    ): FeatureFlagDecisionDto {
        val featureKey = checkNotNull(parameters.featureKey())

        return hackleAppCore.featureFlagDetail(featureKey, parameters.user(), hackleAppContext).toDto()
    }

    private fun track(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext) {
        val event = checkNotNull(parameters.event())
        return hackleAppCore.track(event, parameters.user(), hackleAppContext)
    }

    private fun remoteConfig(parameters: HackleInvokeParameters, hackleAppContext: HackleAppContext): Any {
        val user = parameters.userWithUserId()
        val key = checkNotNull(parameters.key())
        when (checkNotNull(parameters.valueType())) {
            "string" -> {
                val defaultValue = checkNotNull(parameters.defaultStringValue())
                return hackleAppCore.remoteConfig(user, hackleAppContext).getString(key, defaultValue)
            }

            "number" -> {
                val defaultValue = checkNotNull(parameters.defaultNumberValue())
                return hackleAppCore.remoteConfig(user, hackleAppContext).getDouble(key, defaultValue.toDouble())
            }

            "boolean" -> {
                val defaultValue = checkNotNull(parameters.defaultBooleanValue())
                return hackleAppCore.remoteConfig(user, hackleAppContext).getBoolean(key, defaultValue)
            }

            else -> {
                throw IllegalArgumentException("Valid parameter must be provided.")
            }
        }
    }

    private fun setCurrentScreen(parameters: HackleInvokeParameters) {
        val screenName = checkNotNull(parameters.screenName())
        val className = checkNotNull(parameters.className())

        hackleAppCore.setCurrentScreen(Screen(screenName, className))
    }
}
