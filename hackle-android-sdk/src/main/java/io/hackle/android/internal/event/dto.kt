package io.hackle.android.internal.event

import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.utils.safe
import io.hackle.sdk.core.user.IdentifierType

internal data class EventPayloadDto(
    val exposureEvents: List<ExposureEventDto>,
    val trackEvents: List<TrackEventDto>,
)

internal data class ExposureEventDto(
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

internal fun List<UserEvent>.toPayload(): EventPayloadDto {

    val exposures = mutableListOf<ExposureEventDto>()
    val tracks = mutableListOf<TrackEventDto>()
    for (event in this) {
        when (event) {
            is UserEvent.Exposure -> exposures += event.toDto()
            is UserEvent.Track -> tracks += event.toDto()
        }.safe
    }

    return EventPayloadDto(
        exposureEvents = exposures,
        trackEvents = tracks,
    )
}

internal fun UserEvent.Exposure.toDto() = ExposureEventDto(
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
