package io.hackle.android.internal.bridge

import io.hackle.android.HackleApp
import io.hackle.android.internal.bridge.model.BridgeInvocation
import io.hackle.android.internal.bridge.model.BridgeInvocation.Command.*
import io.hackle.android.internal.bridge.model.BridgeResponse
import io.hackle.android.internal.bridge.model.from
import io.hackle.android.internal.bridge.model.toMap
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation

internal class HackleBridge(
    private val app: HackleApp
) {

    fun getAppSdkKey(): String {
        return app.sdk.key
    }

    fun invoke(string: String): String {
        var response: BridgeResponse
        try {
            val invocation = BridgeInvocation(string)
            response = invoke(invocation.command, invocation.parameters)
        } catch (throwable: Throwable) {
            response = BridgeResponse.error(throwable)
        }
        return response.toJsonString()
    }

    private fun invoke(command: BridgeInvocation.Command, parameters: Map<String, Any>): BridgeResponse {
        when (command) {
            GET_SESSION_ID -> {
                return BridgeResponse.success(app.sessionId)
            }
            GET_USER -> {
                val data = app.user.toMap()
                return BridgeResponse.success(data)
            }
            SET_USER -> {
                setUser(parameters)
                return BridgeResponse.success()
            }
            SET_USER_ID -> {
                setUserId(parameters)
                return BridgeResponse.success()
            }
            SET_DEVICE_ID -> {
                setDeviceId(parameters)
                return BridgeResponse.success()
            }
            SET_USER_PROPERTY -> {
                setUserProperty(parameters)
                return BridgeResponse.success()
            }
            UPDATE_USER_PROPERTY -> {
                updateUserProperties(parameters)
                return BridgeResponse.success()
            }
            RESET_USER -> {
                app.resetUser()
                return BridgeResponse.success()
            }
            VARIATION -> {
                val data = variation(parameters)
                return BridgeResponse.success(data)
            }
            VARIATION_DETAIL -> {
                val data = variationDetail(parameters)
                return BridgeResponse.success(data)
            }
            IS_FEATURE_ON -> {
                val data = isFeatureOn(parameters)
                return BridgeResponse.success(data)
            }
            FEATURE_FLAG_DETAIL -> {
                val data = featureFlagDetail(parameters)
                return BridgeResponse.success(data)
            }
            TRACK -> {
                track(parameters)
                return BridgeResponse.success()
            }
            REMOTE_CONFIG -> {
                val data = remoteConfig(parameters)
                return BridgeResponse.success(data)
            }
            SHOW_USER_EXPLORER -> {
                app.showUserExplorer()
                return BridgeResponse.success()
            }
        }
    }

    private fun setUser(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val data = checkNotNull(parameters["user"] as? Map<String, Any>)
        val user = User.from(data)
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
        val data = checkNotNull(parameters["operations"] as? Map<String, Map<String, Any>>)
        val operations = PropertyOperations.from(data)
        app.updateUserProperties(operations)
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
                val user = User.from(data)
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

    private fun variationDetail(parameters: Map<String, Any>): Map<String, Any> {
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
                ).toMap()
            }
        }
        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val user = User.from(data)
                return app.variationDetail(
                    experimentKey = experimentKey.toLong(),
                    user = user,
                    defaultVariation = defaultVariation
                ).toMap()
            }
        }
        return app.variationDetail(
            experimentKey = experimentKey.toLong(),
            defaultVariation = defaultVariation
        ).toMap()
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
                val user = User.from(data)
                return app.isFeatureOn(
                    featureKey = featureKey.toLong(),
                    user = user
                )
            }
        }
        return app.isFeatureOn(featureKey = featureKey.toLong())
    }

    private fun featureFlagDetail(parameters: Map<String, Any>): Map<String, Any> {
        val featureKey = checkNotNull(parameters["featureKey"] as? Number)
        if (parameters["user"] is String) {
            val userId = parameters["user"] as? String
            if (userId != null) {
                return app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    userId = userId
                ).toMap()
            }
        }
        if (parameters["user"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val user = User.from(data)
                return app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    user = user
                ).toMap()
            }
        }
        return app.featureFlagDetail(featureKey = featureKey.toLong()).toMap()
    }

    private fun track(parameters: Map<String, Any>) {
        if (parameters["event"] is String) {
            val eventKey = parameters["event"] as String
            track(eventKey = eventKey, parameters = parameters)
        } else if (parameters["event"] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["event"] as Map<String, Any>
            val event = checkNotNull(Event.from(data))
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
            val user = User.from(data)
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
            val user = User.from(data)
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
            user = User.from(data)
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