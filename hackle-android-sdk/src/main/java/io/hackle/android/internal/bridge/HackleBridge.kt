package io.hackle.android.internal.bridge

import io.hackle.android.HackleAppMode
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.bridge.model.*
import io.hackle.android.internal.bridge.model.BridgeInvocation.Command.*
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.model.Sdk
import io.hackle.sdk.common.*
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations

internal class HackleBridge(
    private val hackleAppCore: HackleAppCore,
    internal val sdk: Sdk,
    internal val mode: HackleAppMode,
) {

    fun invoke(string: String): String {
        val response: BridgeResponse = try {
            val invocation = BridgeInvocation(string)
            invoke(invocation.command, invocation.parameters, invocation.browserProperties)
        } catch (throwable: Throwable) {
            BridgeResponse.error(throwable)
        }
        return response.toJsonString()
    }

    private fun invoke(
        command: BridgeInvocation.Command,
        parameters: HackleBridgeParameters,
        browserProperties: HackleBrowserProperties
    ): BridgeResponse {
        val hackleAppContext = HackleAppContext.create(browserProperties)
        return when (command) {
            GET_SESSION_ID -> {
                BridgeResponse.success(hackleAppCore.sessionId)
            }

            GET_USER -> {
                val data = hackleAppCore.user.toDto()
                BridgeResponse.success(data)
            }

            SET_USER -> {
                setUser(parameters)
                BridgeResponse.success()
            }

            SET_USER_ID -> {
                setUserId(parameters)
                BridgeResponse.success()
            }

            SET_DEVICE_ID -> {
                setDeviceId(parameters)
                BridgeResponse.success()
            }

            SET_USER_PROPERTY -> {
                setUserProperty(parameters, hackleAppContext)
                BridgeResponse.success()
            }

            UPDATE_USER_PROPERTY -> {
                updateUserProperties(parameters, hackleAppContext)
                BridgeResponse.success()
            }

            UPDATE_PUSH_SUBSCRIPTIONS -> {
                updatePushSubscriptions(parameters, hackleAppContext)
                BridgeResponse.success()
            }

            UPDATE_SMS_SUBSCRIPTIONS -> {
                updateSmsSubscriptions(parameters, hackleAppContext)
                BridgeResponse.success()
            }

            UPDATE_KAKAO_SUBSCRIPTIONS -> {
                updateKakaoSubscriptions(parameters, hackleAppContext)
                BridgeResponse.success()
            }

            RESET_USER -> {
                hackleAppCore.resetUser(hackleAppContext, null)
                BridgeResponse.success()
            }

            SET_PHONE_NUMBER -> {
                setPhoneNumber(parameters, hackleAppContext)
                BridgeResponse.success()
            }

            UNSET_PHONE_NUMBER -> {
                hackleAppCore.unsetPhoneNumber(hackleAppContext, null)
                BridgeResponse.success()
            }

            VARIATION -> {
                val data = variation(parameters, hackleAppContext)
                BridgeResponse.success(data)
            }

            VARIATION_DETAIL -> {
                val data = variationDetail(parameters, hackleAppContext)
                BridgeResponse.success(data)
            }

            IS_FEATURE_ON -> {
                val data = isFeatureOn(parameters, hackleAppContext)
                BridgeResponse.success(data)
            }

            FEATURE_FLAG_DETAIL -> {
                val data = featureFlagDetail(parameters, hackleAppContext)
                BridgeResponse.success(data)
            }

            TRACK -> {
                track(parameters, hackleAppContext)
                BridgeResponse.success()
            }

            REMOTE_CONFIG -> {
                val data = remoteConfig(parameters, hackleAppContext)
                BridgeResponse.success(data)
            }

            SET_CURRENT_SCREEN -> {
                setCurrentScreen(parameters)
                BridgeResponse.success()
            }

            SHOW_USER_EXPLORER -> {
                hackleAppCore.showUserExplorer()
                BridgeResponse.success()
            }

            HIDE_USER_EXPLORER -> {
                hackleAppCore.hideUserExplorer()
                BridgeResponse.success()
            }
        }
    }

    private fun setUser(parameters: HackleBridgeParameters) {
        @Suppress("UNCHECKED_CAST")
        val data = checkNotNull(parameters.userAsMap())
        val dto = UserDto.from(data)
        val user = User.from(dto)
        hackleAppCore.setUser(user, null)
    }

    private fun setUserId(parameters: HackleBridgeParameters) {
        check(parameters.containsKey("userId"))
        val userId = parameters.userId()
        hackleAppCore.setUserId(userId, null)
    }

    private fun setDeviceId(parameters: HackleBridgeParameters) {
        val deviceId = checkNotNull(parameters.deviceId())
        hackleAppCore.setDeviceId(deviceId, null)
    }

    private fun setUserProperty(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext) {
        val key = checkNotNull(parameters.key())
        val value = parameters.value()
        val operations = PropertyOperations.builder()
            .set(key, value)
            .build()
        hackleAppCore.updateUserProperties(operations, hackleAppContext, null)
    }

    private fun updateUserProperties(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.propertyOperationDto())
        val operations = PropertyOperations.from(dto)
        hackleAppCore.updateUserProperties(operations, hackleAppContext, null)
    }

    private fun updatePushSubscriptions(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        hackleAppCore.updatePushSubscriptions(operations, hackleAppContext)
    }

    private fun updateSmsSubscriptions(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        hackleAppCore.updateSmsSubscriptions(operations, hackleAppContext)
    }

    private fun updateKakaoSubscriptions(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        hackleAppCore.updateKakaoSubscriptions(operations, hackleAppContext)
    }

    private fun setPhoneNumber(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext) {
        val phoneNumber = checkNotNull(parameters.phoneNumber())
        hackleAppCore.setPhoneNumber(phoneNumber, hackleAppContext, null)
    }

    private fun variation(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext): String {
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

    private fun variationDetail(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext): DecisionDto {
        val experimentKey = checkNotNull(parameters.experimentKey())
        val defaultVariationKey = parameters.defaultVariation()
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)

        return hackleAppCore.variationDetail(experimentKey, parameters.user(), defaultVariation, hackleAppContext)
            .toDto()
    }

    private fun isFeatureOn(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext): Boolean {
        val featureKey = checkNotNull(parameters.featureKey())

        return hackleAppCore.featureFlagDetail(featureKey, parameters.user(), hackleAppContext).isOn
    }

    private fun featureFlagDetail(
        parameters: HackleBridgeParameters,
        hackleAppContext: HackleAppContext
    ): FeatureFlagDecisionDto {
        val featureKey = checkNotNull(parameters.featureKey())

        return hackleAppCore.featureFlagDetail(featureKey, parameters.user(), hackleAppContext).toDto()
    }

    private fun track(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext) {
        val hackleEvent = when (val event = parameters.event()) {
            is String -> {
                Event.of(event)
            }

            is Map<*, *> -> {
                val dto = EventDto.from(event)
                Event.from(dto)
            }

            else -> {
                throw IllegalArgumentException("Valid parameter must be provided.")
            }
        }

        return hackleAppCore.track(hackleEvent, parameters.user(), hackleAppContext)
    }

    private fun remoteConfig(parameters: HackleBridgeParameters, hackleAppContext: HackleAppContext): String {
        val user = when (val user = parameters["user"]) {
            is String -> {
                User.builder()
                    .userId(user)
                    .build()
            }

            is Map<*, *> -> {
                val data = parameters.userAsMap()
                if (data != null) {
                    User.from(UserDto.from(data))
                } else {
                    null
                }
            }

            else -> {
                null
            }
        }
        val remoteConfig = hackleAppCore.remoteConfig(user, hackleAppContext)

        val key = checkNotNull(parameters.key())
        when (checkNotNull(parameters.valueType())) {
            "string" -> {
                val defaultValue = checkNotNull(parameters.defaultValue() as? String)
                return remoteConfig.getString(key, defaultValue)
            }

            "number" -> {
                val defaultValue = checkNotNull(parameters.defaultValue() as? Number)
                return remoteConfig.getDouble(key, defaultValue.toDouble()).toString()
            }

            "boolean" -> {
                val defaultValue = checkNotNull(parameters.defaultValue() as? Boolean)
                return remoteConfig.getBoolean(key, defaultValue).toString()
            }

            else -> {
                throw IllegalArgumentException("Valid parameter must be provided.")
            }
        }
    }

    private fun setCurrentScreen(parameters: HackleBridgeParameters) {
        val screenName = checkNotNull(parameters.screenName())
        val className = checkNotNull(parameters.className())

        hackleAppCore.setCurrentScreen(Screen(screenName, className))
    }
}
