package io.hackle.android.internal.bridge

import io.hackle.android.HackleApp
import io.hackle.android.internal.bridge.model.BridgeInvocation
import io.hackle.android.internal.bridge.model.BridgeInvocation.Command.*
import io.hackle.android.internal.bridge.model.BridgeResponse
import io.hackle.android.internal.bridge.model.EventDto
import io.hackle.android.internal.bridge.model.PropertyOperationsDto
import io.hackle.android.internal.bridge.model.UserDto
import io.hackle.android.internal.bridge.model.from
import io.hackle.android.internal.bridge.model.toDto
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation

@Suppress("DEPRECATION")
internal class HackleBridge(
    private val app: HackleApp
) {

    fun getAppSdkKey(): String {
        return app.sdk.key
    }

    fun invoke(string: String): String {
        val response: BridgeResponse = try {
            val invocation = BridgeInvocation(string)
            invoke(invocation)
        } catch (throwable: Throwable) {
            BridgeResponse.error(throwable)
        }
        return response.toJsonString()
    }

    private fun invoke(invocation: BridgeInvocation): BridgeResponse {
        return when (invocation.command) {
            GET_SESSION_ID -> BridgeResponse.success(app.sessionId)
            GET_USER -> BridgeResponse.success(app.user.toDto())
            SET_USER -> setUser(invocation)
            SET_USER_ID -> setUserId(invocation)
            SET_DEVICE_ID -> setDeviceId(invocation)
            SET_USER_PROPERTY -> setUserProperty(invocation)
            UPDATE_USER_PROPERTY -> updateUserProperties(invocation)
            RESET_USER -> resetUser()
            VARIATION -> variation(invocation)
            VARIATION_DETAIL -> variationDetail(invocation)
            IS_FEATURE_ON -> isFeatureOn(invocation)
            FEATURE_FLAG_DETAIL -> featureFlagDetail(invocation)
            TRACK -> track(invocation)
            REMOTE_CONFIG -> remoteConfig(invocation)
            SHOW_USER_EXPLORER -> showUserExplorer()
        }
    }

    private fun setUser(invocation: BridgeInvocation): BridgeResponse {
        val dto = invocation.getParameterNotNull<UserDto>("user")
        val user = User.from(dto)
        app.setUser(user)
        return BridgeResponse.success()
    }

    private fun setUserId(invocation: BridgeInvocation): BridgeResponse {
        val userId = invocation.getParameterNotNull<String>("userId")
        app.setUserId(userId)
        return BridgeResponse.success()
    }

    private fun setDeviceId(invocation: BridgeInvocation): BridgeResponse {
        val deviceId = invocation.getParameterNotNull<String>("deviceId")
        app.setDeviceId(deviceId)
        return BridgeResponse.success()
    }

    private fun setUserProperty(invocation: BridgeInvocation): BridgeResponse {
        val key = invocation.getParameterNotNull<String>("key")
        val value = invocation.getParameter<String>("value")
        app.setUserProperty(key, value)
        return BridgeResponse.success()
    }

    private fun updateUserProperties(invocation: BridgeInvocation): BridgeResponse {
        val dto = invocation.getParameterNotNull<PropertyOperationsDto>("operations")
        val operations = PropertyOperations.from(dto)
        app.updateUserProperties(operations)
        return BridgeResponse.success()
    }

    private fun resetUser(): BridgeResponse {
        app.resetUser()
        return BridgeResponse.success()
    }

    private fun variation(invocation: BridgeInvocation): BridgeResponse {
        val experimentKey = invocation.getParameterNotNull<Number>("experimentKey")
        val defaultVariationKey = invocation.getParameter<String>("defaultVariation") ?: ""
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)
        if (invocation.hasParameter("user")) {
            val userId = invocation.getParameter<String>("user")
            if (!userId.isNullOrEmpty()) {
                val result = app.variation(
                    experimentKey = experimentKey.toLong(),
                    userId = userId,
                    defaultVariation = defaultVariation
                )
                return BridgeResponse.success(result.name)
            }

            val dto = invocation.getParameter<UserDto>("user")
            if (dto != null) {
                val user = User.from(dto)
                val result = app.variation(
                    experimentKey = experimentKey.toLong(),
                    user = user,
                    defaultVariation = defaultVariation
                )
                return BridgeResponse.success(result.name)
            }
        }
        val result = app.variation(
            experimentKey = experimentKey.toLong(),
            defaultVariation = defaultVariation
        )
        return BridgeResponse.success(result.name)
    }

    private fun variationDetail(invocation: BridgeInvocation): BridgeResponse {
        val experimentKey = invocation.getParameterNotNull<Number>("experimentKey")
        val defaultVariationKey = invocation.getParameter<String>("defaultVariation") ?: ""
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)
        if (invocation.hasParameter("user")) {
            val userId = invocation.getParameter<String>("user")
            if (!userId.isNullOrEmpty()) {
                val result = app.variationDetail(
                    experimentKey = experimentKey.toLong(),
                    userId = userId,
                    defaultVariation = defaultVariation
                ).toDto()
                return BridgeResponse.success(result)
            }

            val dto = invocation.getParameter<UserDto>("user")
            if (dto != null) {
                val user = User.from(dto)
                val result = app.variationDetail(
                    experimentKey = experimentKey.toLong(),
                    user = user,
                    defaultVariation = defaultVariation
                ).toDto()
                return BridgeResponse.success(result)
            }
        }
        val result = app.variationDetail(
            experimentKey = experimentKey.toLong(),
            defaultVariation = defaultVariation
        ).toDto()
        return BridgeResponse.success(result)
    }

    private fun isFeatureOn(invocation: BridgeInvocation): BridgeResponse {
        val featureKey = invocation.getParameterNotNull<Number>("featureKey")
        if (invocation.hasParameter("user")) {
            val userId = invocation.getParameter<String>("user")
            if (!userId.isNullOrEmpty()) {
                val result = app.isFeatureOn(
                    featureKey = featureKey.toLong(),
                    userId = userId
                )
                return BridgeResponse.success(result)
            }

            val dto = invocation.getParameter<UserDto>("user")
            if (dto != null) {
                val user = User.from(dto)
                val result = app.isFeatureOn(
                    featureKey = featureKey.toLong(),
                    user = user
                )
                return BridgeResponse.success(result)
            }
        }
        val result = app.isFeatureOn(featureKey.toLong())
        return BridgeResponse.success(result)
    }

    private fun featureFlagDetail(invocation: BridgeInvocation): BridgeResponse {
        val featureKey = invocation.getParameterNotNull<Number>("featureKey")
        if (invocation.hasParameter("user")) {
            val userId = invocation.getParameter<String>("user")
            if (!userId.isNullOrEmpty()) {
                val result = app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    userId = userId
                ).toDto()
                return BridgeResponse.success(result)
            }

            val dto = invocation.getParameter<UserDto>("user")
            if (dto != null) {
                val user = User.from(dto)
                val result = app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    user = user
                ).toDto()
                return BridgeResponse.success(result)
            }
        }
        val result = app.featureFlagDetail(featureKey.toLong()).toDto()
        return BridgeResponse.success(result)
    }

    private fun track(invocation: BridgeInvocation): BridgeResponse {
        val eventKey = invocation.getParameter<String>("event")
        if (!eventKey.isNullOrEmpty()) {
            return track(eventKey = eventKey, invocation = invocation)
        }

        val dto = invocation.getParameterNotNull<EventDto>("event")
        val event = Event.from(dto)
        return track(event = event, invocation = invocation)
    }

    private fun track(eventKey: String, invocation: BridgeInvocation): BridgeResponse {
        if (invocation.hasParameter("user")) {
            val userId = invocation.getParameter<String>("user")
            if (!userId.isNullOrEmpty()) {
                app.track(eventKey = eventKey, userId = userId)
                return BridgeResponse.success()
            }

            val dto = invocation.getParameter<UserDto>("user")
            if (dto != null) {
                val user = User.from(dto)
                app.track(eventKey = eventKey, user = user)
                return BridgeResponse.success()
            }
        }

        app.track(eventKey)
        return BridgeResponse.success()
    }

    private fun track(event: Event, invocation: BridgeInvocation): BridgeResponse {
        if (invocation.hasParameter("user")) {
            val userId = invocation.getParameter<String>("user")
            if (!userId.isNullOrEmpty()) {
                app.track(event = event, userId = userId)
                return BridgeResponse.success()
            }

            val dto = invocation.getParameter<UserDto>("user")
            if (dto != null) {
                val user = User.from(dto)
                app.track(event = event, user = user)
                return BridgeResponse.success()
            }
        }

        app.track(event)
        return BridgeResponse.success()
    }

    private fun remoteConfig(invocation: BridgeInvocation): BridgeResponse {
        val key = invocation.getParameterNotNull<String>("key")
        val valueType = invocation.getParameterNotNull<String>("valueType")

        var user: User? = null
        if (invocation.hasParameter("user")) {
            val userId = invocation.getParameter<String>("user")
            if (!userId.isNullOrEmpty()) {
                user = User.builder()
                    .userId(userId)
                    .build()
            } else {
                val dto = invocation.getParameter<UserDto>("user")
                if (dto != null) {
                    user = User.from(dto)
                }
            }
        }

        val config: HackleRemoteConfig =
            if (user != null) {
                app.remoteConfig(user)
            } else {
                app.remoteConfig()
            }
        val result = when (valueType) {
            "string" -> {
                val defaultValue = invocation.getParameterNotNull<String>("defaultValue")
                config.getString(key, defaultValue)
            }
            "number" -> {
                val defaultValue = invocation.getParameterNotNull<Number>("defaultValue")
                config.getDouble(key, defaultValue.toDouble()).toString()
            }
            "boolean" -> {
                val defaultValue = invocation.getParameterNotNull<Boolean>("defaultValue")
                config.getBoolean(key, defaultValue).toString()
            }
            else -> {
                throw IllegalArgumentException("Valid parameter must be provided.")
            }
        }
        return BridgeResponse.success(result)
    }

    fun showUserExplorer(): BridgeResponse {
        app.showUserExplorer()
        return BridgeResponse.success()
    }
}