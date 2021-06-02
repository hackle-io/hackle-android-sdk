package io.hackle.android.internal.workspace

import com.google.gson.annotations.SerializedName

internal data class WorkspaceDto(

    @SerializedName("experiments")
    private val _experiments: List<ExperimentDto>?,

    @SerializedName("completedExperiments")
    private val _completedExperiments: List<CompletedExperimentDto>?,

    @SerializedName("buckets")
    private val _buckets: List<BucketDto>?,

    @SerializedName("events")
    private val _events: List<EventTypeDto>?,
) {

    val experiments get() = _experiments ?: emptyList()
    val completedExperiments get() = _completedExperiments ?: emptyList()
    val buckets get() = _buckets ?: emptyList()
    val events get() = _events ?: emptyList()
}

internal data class ExperimentDto(
    val id: Long,
    val key: Long,
    val status: String,
    val bucketId: Long,
    val variations: List<VariationDto>,
    val execution: ExecutionDto,
)

internal data class VariationDto(
    val id: Long,
    val key: String,
    val status: String,
)

internal data class CompletedExperimentDto(
    val experimentId: Long,
    val experimentKey: Long,
    val winnerVariationId: Long,
    val winnerVariationKey: String,
)

internal data class ExecutionDto(
    val status: String,

    @SerializedName("userOverrides")
    private val _userOverrides: List<UserOverrideDto>?,
) {

    val userOverrides get() = _userOverrides ?: emptyList()
}

internal data class UserOverrideDto(
    val userId: String,
    val variationId: Long,
)

internal data class BucketDto(
    val id: Long,
    val seed: Int,
    val slotSize: Int,

    @SerializedName("slots")
    private val _slots: List<SlotDto>?,
) {

    val slots get() = _slots ?: emptyList()
}

internal data class SlotDto(
    val startInclusive: Int,
    val endExclusive: Int,
    val variationId: Long,
)

internal data class EventTypeDto(
    val id: Long,
    val key: String,
)
