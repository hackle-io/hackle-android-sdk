package io.hackle.android.internal.event

import io.hackle.android.internal.database.EventEntity
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.user.IdentifierType

internal fun List<EventEntity>.toBody(): String {
    val exposures = mutableListOf<String>()
    val tracks = mutableListOf<String>()
    for (event in this) {
        when (event.type) {
            EventEntity.Type.EXPOSURE -> exposures.add(event.body)
            EventEntity.Type.TRACK -> tracks.add(event.body)
        }
    }

    val exposurePayload = exposures.joinToString(",", "[", "]")
    val trackPayload = tracks.joinToString(",", "[", "]")

    return "{\"exposureEvents\":$exposurePayload,\"trackEvents\":$trackPayload}"
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
