package io.hackle.android.internal.bridge

import io.hackle.android.HackleApp
import io.hackle.android.internal.utils.parseJson
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.PropertyOperation
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision

internal class HackleBridge(
    private val app: HackleApp
) {

    companion object {
        private const val KEY_HACKLE = "_hackle"
        private const val KEY_COMMAND = "_command"
        private const val KEY_PARAMETERS = "_parameters"
    }

    fun getAppSdkKey(): String {
        return app.sdk.key
    }

    fun invoke(string: String): String? {
        try {
            val data = string.parseJson<Map<String, Any>?>() ?: return null
            @Suppress("UNCHECKED_CAST")
            val invocation = data[KEY_HACKLE] as? Map<String, Any> ?: return null
            val command = invocation[KEY_COMMAND] as? String ?: return null
            @Suppress("UNCHECKED_CAST")
            val parameters = invocation[KEY_PARAMETERS] as? Map<String, Any> ?: HashMap()
            return invoke(command, parameters)
        } catch (throwable: Throwable) {
            return null
        }
    }

    fun invoke(command: String, parameters: Map<String, Any>): String? {
        var returnValue: String? = null
        when (command) {
            "getSessionId" -> returnValue = app.sessionId
            "getUser" -> returnValue = app.user.toJson()
            "setUser" -> setUser(parameters)
            "setUserId" -> setUserId(parameters)
            "setDeviceId" -> setDeviceId(parameters)
            "setUserProperty" -> setUserProperty(parameters)
            "updateUserProperties" -> updateUserProperties(parameters)
            "resetUser" -> app.resetUser()
            "variation" -> returnValue = variation(parameters)
            "variationDetail" -> returnValue = variationDetail(parameters)
            "isFeatureOn" -> returnValue = isFeatureOn(parameters)
            "featureFlagDetail" -> returnValue = featureFlagDetail(parameters)
            "track" -> track(parameters)
            "remoteConfig" -> returnValue = remoteConfig(parameters)
            "showUserExplorer" -> app.showUserExplorer()
        }
        return returnValue
    }

    private fun setUser(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val data = parameters["user"] as? Map<String, Any> ?: return
        val user = data.toUser()
        app.setUser(user)
    }

    private fun setUserId(parameters: Map<String, Any>) {
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            app.setUserId(userId)
        }
    }

    private fun setDeviceId(parameters: Map<String, Any>) {
        if (parameters.containsKey("deviceId")) {
            val deviceId = parameters["deviceId"] as? String ?: return
            app.setDeviceId(deviceId)
        }
    }

    private fun setUserProperty(parameters: Map<String, Any>) {
        val key = parameters["key"] as? String ?: return
        val value = parameters["value"]
        app.setUserProperty(key, value)
    }

    private fun updateUserProperties(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val operations = parameters["operations"] as? Map<String, Map<String, Any>> ?: return
        val builder = PropertyOperations.builder()
        for ((operationText, properties) in operations) {
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
        app.updateUserProperties(builder.build())
    }

    private fun variation(parameters: Map<String, Any>): String? {
        val experimentKey = parameters["experimentKey"] as? Number ?: return null
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

    private fun variationDetail(parameters: Map<String, Any>): String? {
        val experimentKey = parameters["experimentKey"] as? Number ?: return null
        val defaultVariationKey = parameters["defaultVariation"] as? String ?: ""
        val defaultVariation = Variation.fromOrControl(defaultVariationKey)
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                return app.variationDetail(
                    experimentKey = experimentKey.toLong(),
                    userId = userId,
                    defaultVariation = defaultVariation
                ).toMap().toJson()
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
                ).toMap().toJson()
            }
        }
        return app.variationDetail(
            experimentKey = experimentKey.toLong(),
            defaultVariation = defaultVariation
        ).toMap().toJson()
    }

    private fun isFeatureOn(parameters: Map<String, Any>): String? {
        val featureKey = parameters["featureKey"] as? Number ?: return null
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                return app.isFeatureOn(
                    featureKey = featureKey.toLong(),
                    userId = userId
                ).toString()
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
                ).toString()
            }
        }
        return app.isFeatureOn(featureKey = featureKey.toLong()).toString()
    }

    private fun featureFlagDetail(parameters: Map<String, Any>): String? {
        val featureKey = parameters["featureKey"] as? Number ?: return null
        if (parameters.containsKey("userId")) {
            val userId = parameters["userId"] as? String
            if (userId != null) {
                return app.featureFlagDetail(
                    featureKey = featureKey.toLong(),
                    userId = userId
                ).toMap().toJson()
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
                ).toMap().toJson()
            }
        }
        return app.featureFlagDetail(featureKey = featureKey.toLong())
            .toMap().toJson()
    }

    private fun track(parameters: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val data = parameters["event"] as? Map<String, Any> ?: return
        val event = data.toEvent() ?: return
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

    private fun remoteConfig(parameters: Map<String, Any>): String? {
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

        val key = parameters["key"] as? String ?: return null
        val valueType = parameters["valueType"] as? String ?: return null
        when (valueType) {
            "string" -> {
                val defaultValue = parameters["defaultValue"] as? String ?: return null
                return config.getString(key, defaultValue)
            }
            "number" -> {
                val defaultValue = parameters["defaultValue"] as? Number ?: return null
                return config.getDouble(key, defaultValue.toDouble()).toString()
            }
            "boolean" -> {
                val defaultValue = parameters["defaultValue"] as? Boolean ?: return null
                return config.getBoolean(key, defaultValue).toString()
            }
            else -> {
                return null
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

    private fun Decision.toMap(): Map<String, Any> {
        val toReturn: MutableMap<String, Any> = HashMap()
        experiment?.apply {
            toReturn["experiment"] = mapOf<String, Any>("key" to key, "version" to version)
        }
        toReturn["variation"] = variation
        toReturn["reason"] = reason
        toReturn["config"] = parameters
        return toReturn
    }

    private fun FeatureFlagDecision.toMap(): Map<String, Any> {
        val toReturn: MutableMap<String, Any> = HashMap()
        featureFlag?.apply {
            toReturn["featureFlag"] = mapOf<String, Any>("key" to key, "version" to version)
        }
        toReturn["isOn"] = isOn
        toReturn["reason"] = reason
        toReturn["config"] = parameters
        return toReturn
    }
}