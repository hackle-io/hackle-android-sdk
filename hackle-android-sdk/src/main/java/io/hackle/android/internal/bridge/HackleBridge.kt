package io.hackle.android.internal.bridge

import io.hackle.android.HackleApp
import io.hackle.android.internal.bridge.model.*
import io.hackle.android.internal.bridge.model.BridgeInvocation.Command.*
import io.hackle.sdk.common.*
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations

@Suppress("DEPRECATION")
internal class HackleBridge(val app: HackleApp) {

    fun invoke(string: String): String {
        val response: BridgeResponse = try {
            val invocation = BridgeInvocation(string)
            invoke(invocation.command, invocation.parameters)
        } catch (throwable: Throwable) {
            BridgeResponse.error(throwable)
        }
        return response.toJsonString()
    }

    private fun invoke(command: BridgeInvocation.Command, parameters: Map<String, Any>): BridgeResponse {
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
                app.unsetPhoneNumber()
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

    private fun setUser(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val data = checkNotNull(parameters["user"] as? Map<String, Any>)
        val dto = UserDto.from(data)
        val user = User.from(dto)
        app.setUser(user)
    }

    private fun setUserId(parameters: Map<String, Any>) {
        val userId = checkNotNull(parameters["userId"] as? String)
        app.setUserId(userId)
    }

    private fun setDeviceId(parameters: Map<String, Any>) {
        val deviceId = checkNotNull(parameters["deviceId"] as? String)
        app.setDeviceId(deviceId)
    }

    private fun setUserProperty(parameters: Map<String, Any>) {
        val key = checkNotNull(parameters["key"] as? String)
        val value = parameters["value"]
        app.setUserProperty(key, value)
    }

    private fun updateUserProperties(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters["operations"] as? PropertyOperationsDto)
        val operations = PropertyOperations.from(dto)
        app.updateUserProperties(operations)
    }

    private fun updatePushSubscriptions(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters["operations"] as? HackleSubscriptionOperationsDto)
        val operations = HackleSubscriptionOperations.from(dto)
        app.updatePushSubscriptions(operations)
    }

    private fun updateSmsSubscriptions(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters["operations"] as? HackleSubscriptionOperationsDto)
        val operations = HackleSubscriptionOperations.from(dto)
        app.updateSmsSubscriptions(operations)
    }

    private fun updateKakaoSubscriptions(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val dto = checkNotNull(parameters["operations"] as? HackleSubscriptionOperationsDto)
        val operations = HackleSubscriptionOperations.from(dto)
        app.updateKakaoSubscriptions(operations)
    }

    private fun setPhoneNumber(parameters: Map<String, Any>) {
        val phoneNumber = checkNotNull(parameters["phoneNumber"] as? String)
        app.setPhoneNumber(phoneNumber)
    }

    private fun variation(parameters: Map<String, Any>): String {
        val experimentKey = checkNotNull(parameters["experimentKey"] as? Number)
        val defaultVariationKey = parameters["defaultVariation"] as? String ?: ""
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)
        if (parameters["user"] is String) {
            val userId = parameters["user"] as String
            return app.variation(
                experimentKey = experimentKey.toLong(),
                userId = userId,
                defaultVariation = defaultVariation
            ).name
        }

        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val dto = UserDto.from(data)
                val user = User.from(dto)
                return app.variation(
                    experimentKey = experimentKey.toLong(),
                    user = user,
                    defaultVariation = defaultVariation
                ).name
            }
        }
        return app.variation(
            experimentKey = experimentKey.toLong(),
            defaultVariation = defaultVariation
        ).name
    }

    private fun variationDetail(parameters: Map<String, Any>): DecisionDto {
        val experimentKey = parameters["experimentKey"] as? Number
            ?: throw IllegalArgumentException("Valid 'experimentKey' parameter must be provided.")
        val defaultVariationKey = parameters["defaultVariation"] as? String ?: ""
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)
        if (parameters["user"] is String) {
            val userId = parameters["user"] as? String
            if (userId != null) {
                return app.variationDetail(
                    experimentKey = experimentKey.toLong(),
                    userId = userId,
                    defaultVariation = defaultVariation
                ).toDto()
            }
        }
        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val dto = UserDto.from(data)
                val user = User.from(dto)
                return app.variationDetail(
                    experimentKey = experimentKey.toLong(),
                    user = user,
                    defaultVariation = defaultVariation
                ).toDto()
            }
        }
        return app.variationDetail(
            experimentKey = experimentKey.toLong(),
            defaultVariation = defaultVariation
        ).toDto()
    }

    private fun isFeatureOn(parameters: Map<String, Any>): Boolean {
        val featureKey = checkNotNull(parameters["featureKey"] as? Number)
        if (parameters["user"] is String) {
            val userId = parameters["user"] as? String
            if (userId != null) {
                return app.isFeatureOn(
                    featureKey = featureKey.toLong(),
                    userId = userId
                )
            }
        }
        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val dto = UserDto.from(data)
                val user = User.from(dto)
                return app.isFeatureOn(
                    featureKey = featureKey.toLong(),
                    user = user
                )
            }
        }
        return app.isFeatureOn(featureKey = featureKey.toLong())
    }

    private fun featureFlagDetail(parameters: Map<String, Any>): FeatureFlagDecisionDto {
        val featureKey = checkNotNull(parameters["featureKey"] as? Number)
        if (parameters["user"] is String) {
            val userId = parameters["user"] as? String
            if (userId != null) {
                return app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    userId = userId
                ).toDto()
            }
        }
        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val dto = UserDto.from(data)
                val user = User.from(dto)
                return app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    user = user
                ).toDto()
            }
        }
        return app.featureFlagDetail(featureKey = featureKey.toLong()).toDto()
    }

    private fun track(parameters: Map<String, Any>) {
        if (parameters["event"] is String) {
            val eventKey = parameters["event"] as String
            track(eventKey = eventKey, parameters = parameters)
        } else if (parameters["event"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["event"] as Map<String, Any>
            val dto = EventDto.from(data)
            val event = Event.from(dto)
            track(event = event, parameters = parameters)
        } else {
            throw IllegalArgumentException("Valid parameter must be provided.")
        }
    }

    private fun track(eventKey: String, parameters: Map<String, Any>) {
        if (parameters["user"] is String) {
            val userId = parameters["user"] as? String
            if (userId != null) {
                app.track(eventKey = eventKey, userId = userId)
                return
            }
        }
        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as Map<String, Any>
            val dto = UserDto.from(data)
            val user = User.from(dto)
            app.track(eventKey = eventKey, user = user)
            return
        }
        app.track(eventKey = eventKey)
    }

    private fun track(event: Event, parameters: Map<String, Any>) {
        if (parameters["user"] is String) {
            val userId = parameters["user"] as? String
            if (userId != null) {
                app.track(event = event, userId = userId)
                return
            }
        }
        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as Map<String, Any>
            val dto = UserDto.from(data)
            val user = User.from(dto)
            app.track(event = event, user = user)
            return
        }
        app.track(event)
    }

    private fun remoteConfig(parameters: Map<String, Any>): String {
        var user: User? = null
        if (parameters["user"] is String) {
            val userId = parameters["user"] as String
            user = User.builder()
                .userId(userId)
                .build()
        }
        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as Map<String, Any>
            val dto = UserDto.from(data)
            user = User.from(dto)
        }

        val config: HackleRemoteConfig =
            if (user != null) {
                app.remoteConfig(user)
            } else {
                app.remoteConfig()
            }

        val key = checkNotNull(parameters["key"] as? String)
        when (checkNotNull(parameters["valueType"] as? String)) {
            "string" -> {
                val defaultValue = checkNotNull(parameters["defaultValue"] as? String)
                return config.getString(key, defaultValue)
            }

            "number" -> {
                val defaultValue = checkNotNull(parameters["defaultValue"] as? Number)
                return config.getDouble(key, defaultValue.toDouble()).toString()
            }

            "boolean" -> {
                val defaultValue = checkNotNull(parameters["defaultValue"] as? Boolean)
                return config.getBoolean(key, defaultValue).toString()
            }

            else -> {
                throw IllegalArgumentException("Valid parameter must be provided.")
            }
        }
    }
}