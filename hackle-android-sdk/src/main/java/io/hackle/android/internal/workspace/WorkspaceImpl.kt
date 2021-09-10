package io.hackle.android.internal.workspace

import io.hackle.sdk.core.model.Bucket
import io.hackle.sdk.core.model.EventType
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.workspace.Workspace

internal class WorkspaceImpl(
    private val experiments: Map<Long, Experiment>,
    private val featureFlags: Map<Long, Experiment>,
    private val eventTypes: Map<String, EventType>,
    private val buckets: Map<Long, Bucket>,
) : Workspace {

    override fun getEventTypeOrNull(eventTypeKey: String): EventType? {
        return eventTypes[eventTypeKey]
    }

    override fun getFeatureFlagOrNull(featureKey: Long): Experiment? {
        return featureFlags[featureKey]
    }

    override fun getExperimentOrNull(experimentKey: Long): Experiment? {
        return experiments[experimentKey]
    }

    override fun getBucketOrNull(bucketId: Long): Bucket? {
        return buckets[bucketId]
    }

    companion object {
        fun from(dto: WorkspaceDto): Workspace {

            val experiment: Map<Long, Experiment> =
                dto.experiments.asSequence()
                    .mapNotNull { it.toExperimentOrNull(AB_TEST) }
                    .associateBy { it.key }

            val featureFlags: Map<Long, Experiment> =
                dto.featureFlags.asSequence()
                    .mapNotNull { it.toExperimentOrNull(FEATURE_FLAG) }
                    .associateBy { it.key }

            val eventTypes: Map<String, EventType.Custom> =
                dto.events.associate { it.key to it.toEventType() }

            val buckets: Map<Long, Bucket> =
                dto.buckets.associate { it.id to it.toBucket() }

            return WorkspaceImpl(
                experiments = experiment,
                featureFlags = featureFlags,
                eventTypes = eventTypes,
                buckets = buckets,
            )
        }
    }
}
