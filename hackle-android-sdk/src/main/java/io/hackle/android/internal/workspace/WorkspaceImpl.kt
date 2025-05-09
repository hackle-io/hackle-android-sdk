package io.hackle.android.internal.workspace

import io.hackle.sdk.core.model.Bucket
import io.hackle.sdk.core.model.Container
import io.hackle.sdk.core.model.EventType
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.ParameterConfiguration
import io.hackle.sdk.core.model.RemoteConfigParameter
import io.hackle.sdk.core.model.Segment
import io.hackle.sdk.core.workspace.Workspace

internal class WorkspaceImpl(
    override val id: Long,
    override val environmentId: Long,
    override val experiments: List<Experiment>,
    override val featureFlags: List<Experiment>,
    private val eventTypes: Map<String, EventType>,
    private val buckets: Map<Long, Bucket>,
    private val segments: Map<String, Segment>,
    private val containers: Map<Long, Container>,
    private val parameterConfigurations: Map<Long, ParameterConfiguration>,
    private val remoteConfigParameters: Map<String, RemoteConfigParameter>,
    override val inAppMessages: List<InAppMessage>
) : Workspace {

    private val _experiments = experiments.associateBy { it.key }
    private val _featureFlags = featureFlags.associateBy { it.key }
    private val _inAppMessages = inAppMessages.associateBy { it.key }

    override fun getEventTypeOrNull(eventTypeKey: String): EventType? {
        return eventTypes[eventTypeKey]
    }

    override fun getFeatureFlagOrNull(featureKey: Long): Experiment? {
        return _featureFlags[featureKey]
    }

    override fun getExperimentOrNull(experimentKey: Long): Experiment? {
        return _experiments[experimentKey]
    }

    override fun getBucketOrNull(bucketId: Long): Bucket? {
        return buckets[bucketId]
    }

    override fun getContainerOrNull(containerId: Long): Container? {
        return containers[containerId]
    }

    override fun getSegmentOrNull(segmentKey: String): Segment? {
        return segments[segmentKey]
    }

    override fun getParameterConfigurationOrNull(parameterConfigurationId: Long): ParameterConfiguration? {
        return parameterConfigurations[parameterConfigurationId]
    }

    override fun getRemoteConfigParameterOrNull(parameterKey: String): RemoteConfigParameter? {
        return remoteConfigParameters[parameterKey]
    }

    override fun getInAppMessageOrNull(inAppMessageKey: Long): InAppMessage? {
        return _inAppMessages[inAppMessageKey]
    }


    companion object {
        fun from(dto: WorkspaceConfigDto): Workspace {
            val id = dto.workspace.id

            val environmentId = dto.workspace.environment.id

            val experiments: List<Experiment> =
                dto.experiments.mapNotNull { it.toExperimentOrNull(AB_TEST) }

            val featureFlags: List<Experiment> =
                dto.featureFlags.mapNotNull { it.toExperimentOrNull(FEATURE_FLAG) }

            val eventTypes: Map<String, EventType.Custom> =
                dto.events.associate { it.key to it.toEventType() }

            val buckets: Map<Long, Bucket> =
                dto.buckets.associate { it.id to it.toBucket() }

            val segments =
                dto.segments.asSequence()
                    .mapNotNull { it.toSegmentOrNull() }
                    .associateBy { it.key }

            val containers = dto.containers.asSequence()
                .map { it.toContainer() }
                .associateBy { it.id }

            val parameterConfigurations = dto.parameterConfigurations.asSequence()
                .map { it.toParameterConfiguration() }
                .associateBy { it.id }

            val remoteConfigParameters = dto.remoteConfigParameters.asSequence()
                .mapNotNull { it.toRemoteConfigParameterOrNull() }
                .associateBy { it.key }

            val inAppMessages = dto.inAppMessages.mapNotNull { it.toInAppMessageOrNull() }

            return WorkspaceImpl(
                id = id,
                environmentId = environmentId,
                experiments = experiments,
                featureFlags = featureFlags,
                eventTypes = eventTypes,
                buckets = buckets,
                segments = segments,
                containers = containers,
                parameterConfigurations = parameterConfigurations,
                remoteConfigParameters = remoteConfigParameters,
                inAppMessages = inAppMessages
            )
        }
    }
}
