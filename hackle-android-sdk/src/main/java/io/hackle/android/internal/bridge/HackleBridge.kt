package io.hackle.android.internal.bridge

import io.hackle.android.HackleApp
import io.hackle.android.internal.bridge.model.BridgeResponse
import io.hackle.android.internal.bridge.model.DecisionDto
import io.hackle.android.internal.bridge.model.FeatureFlagDecisionDto
import io.hackle.android.internal.bridge.model.Invocation
import io.hackle.android.internal.bridge.model.Invocation.Command
import io.hackle.android.internal.bridge.model.toDto
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.PropertyOperation
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
        try {
            val invocation = Invocation(string)
            val returnValue = invoke(invocation.command, invocation.parameters)
            return BridgeResponse(
                success = true,
                message = "OK",
                data = returnValue
            ).toJson()
        } catch (throwable: Throwable) {
            return BridgeResponse(
                success = false,
                message = throwable.message
            ).toJson()
        }
    }

    private fun invoke(command: Command, parameters: Map<String, Any>): Any? {
        var returnValue: Any? = null
        when (command) {
            Command.GET_SESSION_ID -> returnValue = app.sessionId
            Command.GET_USER -> returnValue = app.user.toDto()
            Command.SET_USER -> setUser(parameters)
            Command.SET_USER_ID -> setUserId(parameters)
            Command.SET_DEVICE_ID -> setDeviceId(parameters)
            Command.SET_USER_PROPERTY -> setUserProperty(parameters)
            Command.UPDATE_USER_PROPERTY -> updateUserProperties(parameters)
            Command.RESET_USER -> app.resetUser()
            Command.VARIATION -> returnValue = variation(parameters)
            Command.VARIATION_DETAIL -> returnValue = variationDetail(parameters)
            Command.IS_FEATURE_ON -> returnValue = isFeatureOn(parameters)
            Command.FEATURE_FLAG_DETAIL -> returnValue = featureFlagDetail(parameters)
            Command.TRACK -> track(parameters)
            Command.REMOTE_CONFIG -> returnValue = remoteConfig(parameters)
            Command.SHOW_USER_EXPLORER -> app.showUserExplorer()
        }
        return returnValue
    }

    private fun setUser(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val data = parameters["user"] as? Map<String, Any>
            ?: throw IllegalArgumentException("Valid 'user' parameter must be provided.")
        val user = data.toUser()
        app.setUser(user)
    }

    private fun setUserId(parameters: Map<String, Any>) {
        val userId = parameters["userId"] as? String
            ?: throw IllegalArgumentException("Valid 'userId' parameter must be provided.")
        app.setUserId(userId)
    }

    private fun setDeviceId(parameters: Map<String, Any>) {
        val deviceId = parameters["deviceId"] as? String
            ?: throw IllegalArgumentException("Valid 'deviceId' parameter must be provided.")
        app.setDeviceId(deviceId)
    }

    private fun setUserProperty(parameters: Map<String, Any>) {
        val key = parameters["key"] as? String
            ?: throw IllegalArgumentException("Valid 'key' parameter must be provided.")
        val value = parameters["value"]
        app.setUserProperty(key, value)
    }

    private fun updateUserProperties(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val operations = parameters["operations"] as? Map<String, Map<String, Any>>
            ?: throw IllegalArgumentException("Valid 'operations' parameter must be provided.")
        val propertyOperations = operations.toPropertyOperations()
        app.updateUserProperties(propertyOperations)
    }

    private fun variation(parameters: Map<String, Any>): String {
        val experimentKey = parameters["experimentKey"] as? Number
            ?: throw IllegalArgumentException("Valid 'experimentKey' parameter must be provided.")
        val defaultVariationKey = parameters["defaultVariation"] as? String ?: ""
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                return app.variation(
                    experimentKey = experimentKey.toLong(),
                    userId = userId,
                    defaultVariation = defaultVariation
                ).name
            }
        }
        if (parameters.containsKey("user")) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val user = data.toUser()
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
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                return app.variationDetail(
                    experimentKey = experimentKey.toLong(),
                    userId = userId,
                    defaultVariation = defaultVariation
                ).toDto()
            }
        }
        if (parameters.containsKey("user")) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val user = data.toUser()
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
        val featureKey = parameters["featureKey"] as? Number
            ?: throw IllegalArgumentException("Valid 'featureKey' parameter must be provided.")
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                return app.isFeatureOn(
                    featureKey = featureKey.toLong(),
                    userId = userId
                )
            }
        }
        if (parameters.containsKey("user")) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val user = data.toUser()
                return app.isFeatureOn(
                    featureKey = featureKey.toLong(),
                    user = user
                )
            }
        }
        return app.isFeatureOn(featureKey = featureKey.toLong())
    }

    private fun featureFlagDetail(parameters: Map<String, Any>): FeatureFlagDecisionDto {
        val featureKey = parameters["featureKey"] as? Number
            ?: throw IllegalArgumentException("Valid 'featureKey' parameter must be provided.")
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                return app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    userId = userId
                ).toDto()
            }
        }
        if (parameters.containsKey("user")) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val user = data.toUser()
                return app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    user = user
                ).toDto()
            }
        }
        return app.featureFlagDetail(featureKey = featureKey.toLong()).toDto()
    }

    private fun track(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val data = parameters["event"] as? Map<String, Any>
        val event = data?.toEvent()
            ?: throw IllegalArgumentException("Valid 'event' parameter must be provided.")
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                app.track(event = event, userId = userId)
                return
            }
        }
        if (parameters.containsKey("user")) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                val user = data.toUser()
                app.track(event = event, user = user)
                return
            }
        }
        app.track(event)
    }

    private fun remoteConfig(parameters: Map<String, Any>): String {
        var user: User? = null
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                user = User.builder()
                    .userId(userId)
                    .build()
            }
        }
        if (parameters.containsKey("user")) {
            @Suppress("UNCHECKED_CAST")
            val data = parameters["user"] as? Map<String, Any>
            if (data != null) {
                user = data.toUser()
            }
        }

        val config: HackleRemoteConfig =
            if (user != null) {
                app.remoteConfig(user)
            } else {
                app.remoteConfig()
            }

        val key = parameters["key"] as? String
            ?: throw IllegalArgumentException("Valid 'key' parameter must be provided.")
        val valueType = parameters["valueType"] as? String
            ?: throw IllegalArgumentException("Valid 'valueType' parameter must be provided.")
        when (valueType) {
            "string" -> {
                val defaultValue = parameters["defaultValue"] as? String
                    ?: throw IllegalArgumentException("Valid 'defaultValue' parameter must be provided.")
                return config.getString(key, defaultValue)
            }
            "number" -> {
                val defaultValue = parameters["defaultValue"] as? Number
                    ?: throw IllegalArgumentException("Valid 'defaultValue' parameter must be provided.")
                return config.getDouble(key, defaultValue.toDouble()).toString()
            }
            "boolean" -> {
                val defaultValue = parameters["defaultValue"] as? Boolean
                    ?: throw IllegalArgumentException("Valid 'defaultValue' parameter must be provided.")
                return config.getBoolean(key, defaultValue).toString()
            }
            else -> {
                throw IllegalArgumentException("Valid 'valueType' parameter must be provided.")
            }
        }
    }

    private fun Map<String, Any>.toUser(): User {
        val builder = User.builder()
        builder.id(this["id"] as? String)
        builder.userId(this["userId"] as? String)
        builder.deviceId(this["deviceId"] as? String)
        @Suppress("UNCHECKED_CAST")
        builder.identifiers(this["identifiers"] as? Map<String, String>)
        @Suppress("UNCHECKED_CAST")
        builder.properties(this["properties"] as? Map<String, Any>)
        return builder.build()
    }

    private fun Map<String, Any>.toEvent(): Event? {
        val key = this["key"] as? String ?: return null
        val builder = Event.builder(key)
        (this["value"] as? Number)?.apply {
            builder.value(this.toDouble())
        }
        @Suppress("UNCHECKED_CAST")
        (this["properties"] as? Map<String, Any>)?.apply {
            builder.properties(this)
        }
        return builder.build()
    }

    private fun Map<String, Map<String, Any>>.toPropertyOperations(): PropertyOperations {
        val builder = PropertyOperations.builder()
        for ((operationText, properties) in this) {
            try {
                when (PropertyOperation.from(operationText)) {
                    PropertyOperation.SET -> properties.forEach { (key, value) -> builder.set(key, value) }
                    PropertyOperation.SET_ONCE -> properties.forEach { (key, value) -> builder.setOnce(key, value) }
                    PropertyOperation.UNSET -> properties.forEach { (key, _) -> builder.unset(key) }
                    PropertyOperation.INCREMENT -> properties.forEach { (key, value) -> builder.increment(key, value) }
                    PropertyOperation.APPEND -> properties.forEach { (key, value) -> builder.append(key, value) }
                    PropertyOperation.APPEND_ONCE -> properties.forEach { (key, value) -> builder.appendOnce(key, value) }
                    PropertyOperation.PREPEND -> properties.forEach { (key, value) -> builder.prepend(key, value) }
                    PropertyOperation.PREPEND_ONCE -> properties.forEach { (key, value) -> builder.prependOnce(key, value) }
                    PropertyOperation.REMOVE -> properties.forEach { (key, value) -> builder.remove(key, value) }
                    PropertyOperation.CLEAR_ALL -> properties.forEach { (_, _) -> builder.clearAll() }
                }
            } catch (_: Throwable) { }
        }
        return builder.build()
    }
}