package io.hackle.android.support

import io.hackle.sdk.common.Event
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.RemoteConfigParameter
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import java.util.UUID

internal object UserEvents {

    fun track(
        key: String,
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        timestamp: Long = 42,
    ): UserEvent.Track {
        return track(event = Event.of(key), user = user, timestamp = timestamp)
    }

    fun track(
        event: Event,
        insertId: String = UUID.randomUUID().toString(),
        timestamp: Long = 42,
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        internalProperties: Map<String, Any> = emptyMap(),
    ): UserEvent.Track {
        return UserEvent.Track(
            insertId = insertId,
            timestamp = timestamp,
            user = user,
            internalProperties = internalProperties,
            event = event
        )
    }

    fun exposure(
        insertId: String = UUID.randomUUID().toString(),
        timestamp: Long = 42,
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        properties: Map<String, Any> = emptyMap(),
        internalProperties: Map<String, Any> = emptyMap(),
        experiment: Experiment = Experiments.config(),
        variationId: Long? = 1,
        variationKey: String = "A",
        decisionReason: DecisionReason = DecisionReason.TRAFFIC_ALLOCATED,
    ): UserEvent.Exposure {
        return UserEvent.Exposure(
            insertId = insertId,
            timestamp = timestamp,
            user = user,
            properties = properties,
            internalProperties = internalProperties,
            experiment = experiment,
            variationId = variationId,
            variationKey = variationKey,
            decisionReason = decisionReason
        )
    }

    fun remoteConfig(
        insertId: String = UUID.randomUUID().toString(),
        timestamp: Long = 42,
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        properties: Map<String, Any> = emptyMap(),
        internalProperties: Map<String, Any> = emptyMap(),
        parameter: RemoteConfigParameter = RemoteConfigs.config(),
        valueId: Long? = 1,
        decisionReason: DecisionReason = DecisionReason.DEFAULT_RULE,
    ): UserEvent.RemoteConfig {
        return UserEvent.RemoteConfig(
            insertId = insertId,
            timestamp = timestamp,
            user = user,
            properties = properties,
            internalProperties = internalProperties,
            parameter = parameter,
            valueId = valueId,
            decisionReason = decisionReason
        )
    }
}
