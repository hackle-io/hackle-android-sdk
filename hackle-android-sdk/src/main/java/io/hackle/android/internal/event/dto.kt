package io.hackle.android.internal.event

import io.hackle.android.internal.database.EventEntity
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.utils.safe
import io.hackle.sdk.core.user.IdentifierType

internal fun List<EventEntity>.toBody(): String {
    val exposures = mutableListOf<String>()
    val tracks = mutableListOf<String>()
    val remoteConfigs = mutableListOf<String>()
    for (event in this) {
        when (event.type) {
            EventEntity.Type.EXPOSURE -> exposures.add(event.body)
            EventEntity.Type.TRACK -> tracks.add(event.body)
            EventEntity.Type.REMOTE_CONFIG -> remoteConfigs.add(event.body)
        }.safe
    }

    val exposurePayload = exposures.joinToString(",", "[", "]")
    val trackPayload = tracks.joinToString(",", "[", "]")
    val remoteConfigPayload = remoteConfigs.joinToString(",", "[", "]")

    return "{\"exposureEvents\":$exposurePayload,\"trackEvents\":$trackPayload,\"remoteConfigEvents\":$remoteConfigPayload}"
}

internal data class ExposureEventDto(
    val insertId: String,
    val timestamp: Long,

    val userId: String?,
    val identifiers: Map<String, String>,
    val userProperties: Map<String, Any>,
    val hackleProperties: Map<String, Any>,

    val experimentId: Long,
    val experimentKey: Long,
    val experimentType: String,
    val experimentVersion: Int,
    val variationId: Long?,
    val variationKey: String,
    val decisionReason: String,
    val properties: Map<String, Any>,
)

internal data class TrackEventDto(
    val insertId: String,
    val timestamp: Long,

    val userId: String?,
    val identifiers: Map<String, String>,
    val userProperties: Map<String, Any>,
    val hackleProperties: Map<String, Any>,

    val eventTypeId: Long,
    val eventTypeKey: String,
    val value: Double?,
    val properties: Map<String, Any>,
)

internal data class RemoteConfigEventDto(
    val insertId: String,
    val timestamp: Long,

    val userId: String?,
    val identifiers: Map<String, String>,
    val userProperties: Map<String, Any>,
    val hackleProperties: Map<String, Any>,

    val parameterId: Long,
    val parameterKey: String,
    val parameterType: String,
    val decisionReason: String,
    val valueId: Long?,
    val properties: Map<String, Any>,
)

internal fun UserEvent.Exposure.toDto() = ExposureEventDto(
    insertId = insertId,
    timestamp = timestamp,

    userId = user.identifiers[IdentifierType.ID.key],
    identifiers = user.identifiers,
    userProperties = user.properties,
    hackleProperties = user.hackleProperties,

    experimentId = experiment.id,
    experimentKey = experiment.key,
    experimentType = experiment.type.name,
    experimentVersion = experiment.version,
    variationId = variationId,
    variationKey = variationKey,
    decisionReason = decisionReason.name,
    properties = properties,
)

internal fun UserEvent.Track.toDto() = TrackEventDto(
    insertId = insertId,
    timestamp = timestamp,

    userId = user.identifiers[IdentifierType.ID.key],
    identifiers = user.identifiers,
    userProperties = user.properties,
    hackleProperties = user.hackleProperties,

    eventTypeId = eventType.id,
    eventTypeKey = eventType.key,
    value = event.value,
    properties = event.properties
)

internal fun UserEvent.RemoteConfig.toDto() = RemoteConfigEventDto(
    insertId = insertId,
    timestamp = timestamp,

    userId = user.identifiers[IdentifierType.ID.key],
    identifiers = user.identifiers,
    userProperties = user.properties,
    hackleProperties = user.hackleProperties,

    parameterId = parameter.id,
    parameterKey = parameter.key,
    parameterType = parameter.type.name,
    valueId = valueId,
    decisionReason = decisionReason.name,
    properties = properties
)
