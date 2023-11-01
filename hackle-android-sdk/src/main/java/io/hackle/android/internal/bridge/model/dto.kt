package io.hackle.android.internal.bridge.model

import com.google.gson.annotations.SerializedName
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertyOperation
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision

internal data class UserDto(
    @SerializedName(KEY_ID)
    val id: String?,
    @SerializedName(KEY_USER_ID)
    val userId: String?,
    @SerializedName(KEY_DEVICE_ID)
    val deviceId: String?,
    @SerializedName(KEY_IDENTIFIERS)
    val identifiers: Map<String, String>,
    @SerializedName(KEY_PROPERTIES)
    val properties: Map<String, Any>
) {

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_USER_ID = "userId"
        private const val KEY_DEVICE_ID = "deviceId"
        private const val KEY_IDENTIFIERS = "identifiers"
        private const val KEY_PROPERTIES = "properties"

        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): UserDto {
            return UserDto(
                id = map[KEY_ID] as? String,
                userId = map[KEY_USER_ID] as? String,
                deviceId = map[KEY_DEVICE_ID] as? String,
                identifiers = map[KEY_IDENTIFIERS] as? Map<String, String> ?: HashMap(),
                properties = map[KEY_PROPERTIES] as? Map<String, Any> ?: HashMap()
            )
        }
    }
}

internal data class DecisionDto(
    @SerializedName("variation")
    val variation: String,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("config")
    val config: Map<String, Any>
)

internal data class FeatureFlagDecisionDto(
    @SerializedName("isOn")
    val isOn: Boolean,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("config")
    val config: Map<String, Any>
)

internal data class EventDto(
    @SerializedName(KEY_KEY)
    val key: String,
    @SerializedName(KEY_VALUE)
    val value: Double?,
    @SerializedName(KEY_PROPERTIES)
    val properties: Map<String, Any>?
) {

    companion object {

        private const val KEY_KEY = "key"
        private const val KEY_VALUE = "value"
        private const val KEY_PROPERTIES = "properties"

        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): EventDto {
            return EventDto(
                key = checkNotNull(map[KEY_KEY] as? String),
                value = (map[KEY_VALUE] as? Number)?.toDouble(),
                properties = map[KEY_PROPERTIES] as? Map<String, Any>
            )
        }
    }
}

typealias PropertyOperationsDto = Map<String, Map<String, Any>>

internal fun User.Companion.from(dto: UserDto): User {
    val builder = builder()
    builder.id(dto.id)
    builder.userId(dto.userId)
    builder.deviceId(dto.deviceId)
    builder.identifiers(dto.identifiers)
    builder.properties(dto.properties)
    return builder.build()
}

internal fun User.toDto() = UserDto(
    id = id,
    userId = userId,
    deviceId = deviceId,
    identifiers = identifiers,
    properties = properties
)

internal fun Decision.toDto() = DecisionDto(
    variation = variation.toString(),
    reason = reason.toString(),
    config = mapOf("parameters" to config.parameters)
)

internal fun FeatureFlagDecision.toDto() = FeatureFlagDecisionDto(
    isOn = isOn,
    reason = reason.toString(),
    config = mapOf("parameters" to config.parameters)
)

internal fun Event.Companion.from(dto: EventDto): Event {
    val builder = builder(dto.key)
    dto.value?.apply { builder.value(this) }
    dto.properties?.apply { builder.properties(this) }
    return builder.build()
}

internal fun PropertyOperations.Companion.from(dto: PropertyOperationsDto): PropertyOperations {
    val builder = builder()
    for ((operationText, properties) in dto) {
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