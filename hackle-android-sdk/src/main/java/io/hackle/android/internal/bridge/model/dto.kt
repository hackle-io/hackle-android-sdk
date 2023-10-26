package io.hackle.android.internal.bridge.model

import io.hackle.android.internal.utils.filterNotNull
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertyOperation
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision

internal fun User.toMap(): Map<String, Any> {
    val toReturn: MutableMap<String, Any?> = HashMap()
    toReturn["id"] = id
    toReturn["userId"] = userId
    toReturn["deviceId"] = deviceId
    toReturn["identifiers"] = identifiers
    toReturn["properties"] = properties
    return toReturn.filterNotNull()
}

internal fun User.Companion.from(map: Map<String, Any>): User {
    val builder = builder()
    builder.id(map["id"] as? String)
    builder.userId(map["userId"] as? String)
    builder.deviceId(map["deviceId"] as? String)
    @Suppress("UNCHECKED_CAST")
    builder.identifiers(map["identifiers"] as? Map<String, String>)
    @Suppress("UNCHECKED_CAST")
    builder.properties(map["properties"] as? Map<String, Any>)
    return builder.build()
}

internal fun Decision.toMap(): Map<String, Any> {
    val toReturn: MutableMap<String, Any> = HashMap()
    toReturn["variation"] = variation
    toReturn["reason"] = reason
    toReturn["config"] = mapOf("parameters" to parameters)
    return toReturn
}

internal fun FeatureFlagDecision.toMap(): Map<String, Any> {
    val toReturn: MutableMap<String, Any> = HashMap()
    toReturn["isOn"] = isOn
    toReturn["reason"] = reason
    toReturn["config"] = mapOf("parameters" to parameters)
    return toReturn
}

internal fun Event.Companion.from(map: Map<String, Any>): Event? {
    val key = map["key"] as? String ?: return null
    val builder = builder(key)
    (map["value"] as? Number)?.apply {
        builder.value(toDouble())
    }
    @Suppress("UNCHECKED_CAST")
    (map["properties"] as? Map<String, Any>)?.apply {
        builder.properties(this)
    }
    return builder.build()
}

internal fun PropertyOperations.Companion.from(map: Map<String, Map<String, Any>>): PropertyOperations {
    val builder = builder()
    for ((operationText, properties) in map) {
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