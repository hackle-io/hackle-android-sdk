package io.hackle.android.internal.workspace.evaluation.model

import io.hackle.android.internal.workspace.config.*

internal class WorkspaceEvaluationContextDto(
    val key: Map<String, String>, // identifiers
    val evaluation: WorkspaceEvaluationDto,
    val fullEvaluatedAt: Long
)

internal class WorkspaceEvaluationDto(
    val workspace: WorkspaceDto,
    val metadata: WorkspaceEvaluationMetadataDto,
    val results: List<EvaluateResultDto>,
)

internal class WorkspaceEvaluationMetadataDto(
    val hash: Int,
    override val evaluatedAt: Long,
    val user: HackleUserMetadataDto,
    override val config: WorkspaceConfigMetadataDto,
) : EvaluationMetadataDto

internal class HackleUserMetadataDto(
    val hash: Int,
)

internal class WorkspaceConfigMetadataDto(
    val modifiedAt: String,
)

internal class EvaluateResultDto(
    val type: String,
    val id: Long,
    val hash: Int,

    val experiment: ExperimentEvaluateResultDto?,
    val featureFlag: ExperimentEvaluateResultDto?,
    val remoteConfig: RemoteConfigParameterEvaluateResultDto?,
    val inAppMessage: InAppMessageEligibilityEvaluateResultDto?,
)

internal class ExperimentEvaluateResultDto(
    val id: Long,
    val key: Long,
    val order: Long,
    val version: Int,
    val execution: ExecutionDto,

    val variation: VariationDto,
    val config: ParameterConfigurationDto?,

    val reason: String,
    val references: List<EntityDto>,
) {
    class ExecutionDto(
        val status: String,
        val version: Int,
    )
}

internal class RemoteConfigParameterEvaluateResultDto(
    val id: Long,
    val key: String,
    val valueType: String,
    val value: RemoteConfigParameterDto.ValueDto?,
    val reason: String,
    val references: List<EntityDto>,
)

internal class InAppMessageEligibilityEvaluateResultDto(
    val id: Long,
    val key: Long,
    val order: Long,

    // Period
    val period: InAppMessageDto.PeriodDto?,
    val timetable: InAppMessageDto.TimetableDto?,

    // EventTrigger
    val eventTriggerRules: List<InAppMessageDto.EventTriggerRuleDto>,
    val eventFrequencyCap: InAppMessageDto.EventFrequencyCapDto?,
    val eventTriggerDelay: InAppMessageDto.CampaignDelayDto?,

    // EvaluateContext
    val evaluateContext: InAppMessageDto.EvaluateContextDto?,

    // MessageContext
    val messageContext: InAppMessageDto.MessageContextDto,

    // Result
    val isEligible: Boolean,
    val layout: InAppMessageLayoutEvaluateResultDto,
    val reason: String,
    val references: List<EntityDto>,
)

internal class InAppMessageLayoutEvaluateResultDto(
    val message: InAppMessageDto.MessageContextDto.MessageDto,
    val reason: String,
    val references: List<EntityDto>,
)

internal class EntityDto(
    val type: String,
    val id: Long,
)

internal class EvaluateEntityDto(
    val type: String,
    val id: Long,
    val hash: Int,
)

internal class RemoteEvaluateContextDto(
    val user: HackleUserDto,
    val operations: Map<String, Map<String, Any>>,
)

internal class HackleUserDto(
    val identifiers: Map<String, String>,
    val userProperties: Map<String, Any>,
    val hackleProperties: Map<String, Any>,
)

// HTTP Request, Response

internal class WorkspaceEvaluateRequestDto(
    val policy: String, // AUTO, FORCE_FULL
    val context: RemoteEvaluateContextDto,
    val base: BaseEvaluationDto?
)

internal class BaseEvaluationDto(
    val fullEvaluatedAt: Long,
    val metadata: WorkspaceEvaluationMetadataDto,
    val entities: List<EvaluateEntityDto>,
)

internal class WorkspaceEvaluateResponseDto(
    val status: String, // FULL, DELTA
    val full: WorkspaceEvaluationDto?,
    val delta: WorkspaceEvaluationDeltaDto?,
)

internal class WorkspaceEvaluationDeltaDto(
    val workspace: WorkspaceDto,
    val metadata: WorkspaceEvaluationMetadataDto,
    val changed: List<EvaluateResultDto>,
    val deleted: List<EntityDto>,
)

internal class EntityEvaluateRequestDto(
    val context: RemoteEvaluateContextDto,
    val entities: List<EntityDto>,
)

internal class EntityEvaluateResponseDto(
    val evaluation: EntityEvaluationDto
)

internal class EntityEvaluationDto(
    val workspace: WorkspaceDto,
    val metadata: EntityEvaluationMetadataDto,
    val results: List<EvaluateResultDto>,
)

internal class EntityEvaluationMetadataDto(
    override val evaluatedAt: Long,
    override val config: WorkspaceConfigMetadataDto,
) : EvaluationMetadataDto

internal interface EvaluationMetadataDto {
    val evaluatedAt: Long
    val config: WorkspaceConfigMetadataDto
}