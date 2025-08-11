package io.hackle.android.internal.bridge

import io.hackle.android.HackleAppMode
import io.hackle.android.internal.HackleAppInternal
import io.hackle.android.internal.bridge.model.*
import io.hackle.android.internal.bridge.model.BridgeInvocation.Command.*
import io.hackle.android.internal.model.Sdk
import io.hackle.sdk.common.*
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations

internal class HackleBridge(
    private val app: HackleAppInternal,
    internal val sdk: Sdk,
    internal val mode: HackleAppMode,
) {

    fun invoke(string: String): String {
        val response: BridgeResponse = try {
            val invocation = BridgeInvocation(string)
            app.withBrowserProperties(invocation.browserProperties) {
                invoke(invocation.command, invocation.parameters)
            }
        } catch (throwable: Throwable) {
            BridgeResponse.error(throwable)
        }
        return response.toJsonString()
    }

    private fun invoke(command: BridgeInvocation.Command, parameters: HackleBridgeParameters): BridgeResponse {
        return when (command) {
            GET_SESSION_ID -> {
                BridgeResponse.success(app.sessionId)
            }

            GET_USER -> {
                val data = app.user.toDto()
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
                setUserProperty(parameters)
                BridgeResponse.success()
            }

            UPDATE_USER_PROPERTY -> {
                updateUserProperties(parameters)
                BridgeResponse.success()
            }

            UPDATE_PUSH_SUBSCRIPTIONS -> {
                updatePushSubscriptions(parameters)
                BridgeResponse.success()
            }

            UPDATE_SMS_SUBSCRIPTIONS -> {
                updateSmsSubscriptions(parameters)
                BridgeResponse.success()
            }

            UPDATE_KAKAO_SUBSCRIPTIONS -> {
                updateKakaoSubscriptions(parameters)
                BridgeResponse.success()
            }

            RESET_USER -> {
                app.resetUser()
                BridgeResponse.success()
            }

            SET_PHONE_NUMBER -> {
                setPhoneNumber(parameters)
                BridgeResponse.success()
            }

            UNSET_PHONE_NUMBER -> {
                app.unsetPhoneNumber(null)
                BridgeResponse.success()
            }

            VARIATION -> {
                val data = variation(parameters)
                BridgeResponse.success(data)
            }

            VARIATION_DETAIL -> {
                val data = variationDetail(parameters)
                BridgeResponse.success(data)
            }

            IS_FEATURE_ON -> {
                val data = isFeatureOn(parameters)
                BridgeResponse.success(data)
            }

            FEATURE_FLAG_DETAIL -> {
                val data = featureFlagDetail(parameters)
                BridgeResponse.success(data)
            }

            TRACK -> {
                track(parameters)
                BridgeResponse.success()
            }

            REMOTE_CONFIG -> {
                val data = remoteConfig(parameters)
                BridgeResponse.success(data)
            }

            SET_CURRENT_SCREEN -> {
                setCurrentScreen(parameters)
                BridgeResponse.success()
            }

            SHOW_USER_EXPLORER -> {
                app.showUserExplorer()
                BridgeResponse.success()
            }

            HIDE_USER_EXPLORER -> {
                app.hideUserExplorer()
                BridgeResponse.success()
            }
        }
    }

    private fun setUser(parameters: HackleBridgeParameters) {
        @Suppress("UNCHECKED_CAST")
        val data = checkNotNull(parameters.userAsMap())
        val dto = UserDto.from(data)
        val user = User.from(dto)
        app.setUser(user)
    }

    private fun setUserId(parameters: HackleBridgeParameters) {
        check(parameters.containsKey("userId"))
        val userId = parameters.userId()
        app.setUserId(userId)
    }

    private fun setDeviceId(parameters: HackleBridgeParameters) {
        val deviceId = checkNotNull(parameters.deviceId())
        app.setDeviceId(deviceId)
    }

    private fun setUserProperty(parameters: HackleBridgeParameters) {
        val key = checkNotNull(parameters.key())
        val value = parameters.value()
        val operations = PropertyOperations.builder()
            .set(key, value)
            .build()
        app.updateUserProperties(operations, null)
    }

    private fun updateUserProperties(parameters: HackleBridgeParameters) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.propertyOperationDto())
        val operations = PropertyOperations.from(dto)
        app.updateUserProperties(operations, null)
    }

    private fun updatePushSubscriptions(parameters: HackleBridgeParameters) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        app.updatePushSubscriptions(operations)
    }

    private fun updateSmsSubscriptions(parameters: HackleBridgeParameters) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        app.updateSmsSubscriptions(operations)
    }

    private fun updateKakaoSubscriptions(parameters: HackleBridgeParameters) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters.hackleSubscriptionOperationDto())
        val operations = HackleSubscriptionOperations.from(dto)
        app.updateKakaoSubscriptions(operations)
    }

    private fun setPhoneNumber(parameters: HackleBridgeParameters) {
        val phoneNumber = checkNotNull(parameters.phoneNumber())
        app.setPhoneNumber(phoneNumber, null)
    }

    private fun variation(parameters: HackleBridgeParameters): String {
        val experimentKey = checkNotNull(parameters.experimentKey())
        val defaultVariationKey = parameters.defaultVariation()
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)
        
        return withUserContext(
            parameters = parameters,
            onDefault = { app.variationDetail(experimentKey, null, defaultVariation).variation.name },
            onUserId = { userId -> app.variationDetail(experimentKey, User.of(userId), defaultVariation).variation.name },
            onUser = { user -> app.variationDetail(experimentKey, user, defaultVariation).variation.name }
        )
    }

    private fun variationDetail(parameters: HackleBridgeParameters): DecisionDto {
        val experimentKey = checkNotNull(parameters.experimentKey())
        val defaultVariationKey = parameters.defaultVariation()
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)
        
        return withUserContext(
            parameters = parameters,
            onDefault = { app.variationDetail(experimentKey, null, defaultVariation).toDto() },
            onUserId = { userId -> app.variationDetail(experimentKey, User.of(userId), defaultVariation).toDto() },
            onUser = { user -> app.variationDetail(experimentKey, user, defaultVariation).toDto() }
        )
    }

    private fun isFeatureOn(parameters: HackleBridgeParameters): Boolean {
        val featureKey = checkNotNull(parameters.featureKey())

        return withUserContext(
            parameters = parameters,
            onDefault = { app.featureFlagDetail(featureKey, null).isOn },
            onUserId = { userId -> app.featureFlagDetail(featureKey, User.of(userId)).isOn },
            onUser = { user -> app.featureFlagDetail(featureKey, user).isOn }
        )
    }

    private fun featureFlagDetail(parameters: HackleBridgeParameters): FeatureFlagDecisionDto {
        val featureKey = checkNotNull(parameters.featureKey())
        
        return withUserContext(
            parameters = parameters,
            onDefault = { app.featureFlagDetail(featureKey, null).toDto() },
            onUserId = { userId -> app.featureFlagDetail(featureKey, User.of(userId)).toDto() },
            onUser = { user -> app.featureFlagDetail(featureKey, user).toDto() }
        )
    }

    private fun track(parameters: HackleBridgeParameters) {
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
        
        return withUserContext(
            parameters = parameters,
            onDefault = { app.track(hackleEvent, null) },
            onUserId = { userId -> app.track(hackleEvent, User.of(userId)) },
            onUser = { user -> app.track(hackleEvent, user)}
        )
    }

    private fun remoteConfig(parameters: HackleBridgeParameters): String {
        val remoteConfig = withUserContext(
            parameters = parameters,
            onDefault = { app.remoteConfig(null) },
            onUserId = { userId -> 
                val user = User.builder()
                    .userId(userId)
                    .build()
                app.remoteConfig(user)
            },
            onUser = { user -> app.remoteConfig(user)}
        )

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
        
        app.setCurrentScreen(Screen(screenName, className))
    }

    /**
     * 파라미터에 포함된 사용자 정보에 따라 적절한 동작을 실행하는 고차 함수.
     * @param R 반환될 결과의 타입
     * @param parameters 원본 파라미터 맵
     * @param onDefault 사용자가 없는 경우 실행할 동작
     * @param onUserId 사용자 ID가 있는 경우 실행할 동작
     * @param onUser 사용자 객체가 있는 경우 실행할 동작
     * @return 각 람다에서 반환된 결과
     */
    private fun <R> withUserContext(
        parameters: HackleBridgeParameters,
        onDefault: () -> R,
        onUserId: (userId: String) -> R,
        onUser: (user: User) -> R
    ): R {
        return when (val userParam = parameters.user()) {
            is String -> {
                onUserId(userParam)
            }
            is Map<*, *> -> {
                val data = parameters.userAsMap()
                if (data != null) {
                    val user = User.from(UserDto.from(data))
                    onUser(user)
                } else {
                    onDefault() // Map이지만 User 객체로 변환 실패 시 기본 동작 실행
                }
            }
            else -> {
                onDefault()
            }
        }
    }
}
