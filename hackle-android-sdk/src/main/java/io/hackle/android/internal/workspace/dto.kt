package io.hackle.android.internal.workspace

internal data class WorkspaceDto(
    val experiments: List<ExperimentDto>,
    val completedExperiments: List<CompletedExperimentDto>,
    val buckets: List<BucketDto>,
    val events: List<EventTypeDto>
)

internal data class ExperimentDto(
    val id: Long,
    val key: Long,
    val status: String,
    val bucketId: Long,
    val variations: List<VariationDto>,
    val execution: ExecutionDto
)

internal data class VariationDto(
    val id: Long,
    val key: String,
    val status: String
)

internal data class CompletedExperimentDto(
    val experimentId: Long,
    val experimentKey: Long,
    val winnerVariationId: Long,
    val winnerVariationKey: String
)

internal data class ExecutionDto(
    val status: String,
    val userOverrides: List<UserOverrideDto>
)

internal data class UserOverrideDto(
    val userId: String,
    val variationId: Long
)

internal data class BucketDto(
    val id: Long,
    val seed: Int,
    val slotSize: Int,
    val slots: List<SlotDto>
)

internal data class SlotDto(
    val startInclusive: Int,
    val endExclusive: Int,
    val variationId: Long
)

internal data class EventTypeDto(
    val id: Long,
    val key: String
)
