package io.hackle.android.internal.workspace

import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.workspace.Workspace

internal class WorkspaceImpl(
    override val experiments: List<Experiment>,
    override val featureFlags: List<Experiment>,
    private val eventTypes: Map<String, EventType>,
    private val buckets: Map<Long, Bucket>,
    private val segments: Map<String, Segment>,
    private val containers: Map<Long, Container>,
) : Workspace {

    private val _experiments = experiments.associateBy { it.key }
    private val _featureFlags = featureFlags.associateBy { it.key }

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

    companion object {
        fun from(dto: WorkspaceDto): Workspace {

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
                .mapNotNull { it.toContainer() }
                .associateBy { it.id }

            return WorkspaceImpl(
                experiments = experiments,
                featureFlags = featureFlags,
                eventTypes = eventTypes,
                buckets = buckets,
                segments = segments,
                containers = containers
            )
        }
    }
}
