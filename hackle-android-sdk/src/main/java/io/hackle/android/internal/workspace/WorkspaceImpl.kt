package io.hackle.android.internal.workspace

import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.workspace.Workspace

internal class WorkspaceImpl(
    private val experiments: Map<Long, Experiment>,
    private val featureFlags: Map<Long, Experiment>,
    private val eventTypes: Map<String, EventType>,
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

    companion object {

        private val log = Logger<WorkspaceImpl>()

        fun from(dto: WorkspaceDto): Workspace {
            val buckets: Map<Long, Bucket> =
                dto.buckets.associate { it.id to it.toBucket() }

            val experiment: Map<Long, Experiment> =
                dto.experiments.asSequence()
                    .mapNotNull { it.toExperiment(AB_TEST, buckets.getValue(it.bucketId)) }
                    .associateBy { it.key }

            val featureFlags: Map<Long, Experiment> =
                dto.featureFlags.asSequence()
                    .mapNotNull { it.toExperiment(FEATURE_FLAG, buckets.getValue(it.bucketId)) }
                    .associateBy { it.key }

            val eventTypes: Map<String, EventType.Custom> =
                dto.events.associate { it.key to it.toEventType() }

            return WorkspaceImpl(
                experiments = experiment,
                featureFlags = featureFlags,
                eventTypes = eventTypes
            )
        }

        private fun ExperimentDto.toExperiment(type: Experiment.Type, bucket: Bucket): Experiment? {

            val variations = variations.associate { it.id to it.toVariation() }
            val overrides = execution.userOverrides.associate { it.userId to it.variationId }

            return when (execution.status) {
                "READY" -> Experiment.Draft(
                    id = id,
                    key = key,
                    type = type,
                    variations = variations,
                    overrides = overrides
                )
                "RUNNING" -> Experiment.Running(
                    id = id,
                    key = key,
                    type = type,
                    bucket = bucket,
                    variations = variations,
                    overrides = overrides
                )
                "PAUSED" -> Experiment.Paused(
                    id = id,
                    key = key,
                    type = type,
                    variations = variations,
                    overrides = overrides
                )
                "STOPPED" -> Experiment.Completed(
                    id = id,
                    key = key,
                    type = type,
                    variations = variations,
                    overrides = overrides,
                    winnerVariationId = requireNotNull(winnerVariationId)
                )
                else -> {
                    log.warn { "Unknown experiment status [$status]" }
                    null
                }
            }
        }

        private fun VariationDto.toVariation() = Variation(
            id = id,
            key = key,
            isDropped = status == "DROPPED"
        )

        private fun BucketDto.toBucket() = Bucket(
            seed = seed,
            slotSize = slotSize,
            slots = slots.map { it.toSlot() }
        )

        private fun SlotDto.toSlot() = Slot(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            variationId = variationId
        )

        private fun EventTypeDto.toEventType() = EventType.Custom(id, key)
    }
}
