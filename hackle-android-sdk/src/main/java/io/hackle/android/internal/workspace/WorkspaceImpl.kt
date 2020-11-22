package io.hackle.android.internal.workspace

import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.workspace.Workspace

internal class WorkspaceImpl(
    private val experiments: Map<Long, Experiment>,
    private val eventTypes: Map<String, EventType>
) : Workspace {

    override fun getEventTypeOrNull(eventTypeKey: String): EventType? {
        return eventTypes[eventTypeKey]
    }

    override fun getExperimentOrNull(experimentKey: Long): Experiment? {
        return experiments[experimentKey]
    }

    companion object {

        private val log = Logger<WorkspaceImpl>()

        fun from(dto: WorkspaceDto): Workspace {
            val buckets: Map<Long, Bucket> =
                dto.buckets.associate { it.id to it.toBucket() }

            val running: Map<Long, Experiment> =
                dto.experiments.asSequence()
                    .mapNotNull { it.toExperimentOrNull(buckets.getValue(it.bucketId)) }
                    .associateBy { it.key }

            val completed: Map<Long, Experiment> =
                dto.completedExperiments.associate { it.experimentKey to it.toExperiment() }

            val eventTypes: Map<String, EventType.Custom> =
                dto.events.associate { it.key to it.toEventType() }

            return WorkspaceImpl(
                experiments = running + completed,
                eventTypes = eventTypes
            )
        }

        private fun ExperimentDto.toExperimentOrNull(bucket: Bucket): Experiment? {
            return when (execution.status) {
                "RUNNING" -> toRunning(bucket)
                else -> {
                    log.warn { "Unknown experiment status [$status]" }
                    null
                }
            }
        }

        private fun ExperimentDto.toRunning(bucket: Bucket): Experiment.Running {
            val variations: Map<Long, Variation> =
                variations.associate { it.id to it.toVariation() }
            return Experiment.Running(
                id = id,
                key = key,
                bucket = bucket,
                variations = variations,
                overrides = execution.userOverrides.associate { it.userId to it.variationId }
            )
        }

        private fun CompletedExperimentDto.toExperiment(): Experiment {
            return Experiment.Completed(
                id = experimentId,
                key = experimentKey,
                winnerVariationKey = winnerVariationKey
            )
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
