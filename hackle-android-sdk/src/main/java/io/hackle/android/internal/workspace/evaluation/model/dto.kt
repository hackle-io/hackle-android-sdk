package io.hackle.android.internal.workspace.evaluation.model

import io.hackle.android.internal.workspace.config.InAppMessageDto
import io.hackle.android.internal.workspace.config.ParameterConfigurationDto
import io.hackle.android.internal.workspace.config.WorkspaceDto

internal class WorkspaceEvaluationRecordDto(
    val key: Map<String, String>, // identifiers
    val evaluation: WorkspaceEvaluationDto,
)

internal class WorkspaceEvaluationDto(
    val workspace: WorkspaceDto,
    val items: List<EvaluateResultDto>,
    val metadata: WorkspaceEvaluationMetadataDto,
)

internal class WorkspaceEvaluationMetadataDto(
    val evaluatedAt: Long,
    val results: WorkspaceEvaluateResultsMetadataDto,
    val user: HackleUserMetadataDto,
    val config: WorkspaceConfigMetadataDto,
)

internal class WorkspaceEvaluateResultsMetadataDto(
    val hash: Int,
)

internal class HackleUserMetadataDto(
    val hash: Int,
)

internal class WorkspaceConfigMetadataDto(
    val lastModified: String,
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
    val version: Int,
    val executionVersion: Int,

    val variationId: Long?,
    val variationKey: String,
    val config: ParameterConfigurationDto?,

    val reason: String,
    val references: List<EntityDto>,
)

internal class RemoteConfigParameterEvaluateResultDto(
    val id: Long,
    val key: String,
    val valueType: String,
    val value: Any,
    val valueId: Long?,
    val reason: String,
    val references: List<EntityDto>,
)

internal class InAppMessageEligibilityEvaluateResultDto(
    val id: Long,
    val key: Long,
    val platformTypes: List<String>,

    // Period
    val period: InAppMessageDto.PeriodDto?,
    val timetable: InAppMessageDto.TimetableDto?,

    // EventTrigger
    val eventTriggerRules: List<InAppMessageDto.EventTriggerRuleDto>,
    val eventFrequencyCap: InAppMessageDto.EventFrequencyCapDto?,
    val eventTriggerDelay: InAppMessageDto.CampaignDelayDto?,

    // EvaluateContext
    val evaluateContext: InAppMessageDto.EvaluateContextDto,

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
    val exposure: InAppMessageDto.MessageContextDto.MessageDto.ExposureDto,
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
    val hash: Int?,
)

internal class RemoteEvaluateContextDto(
    val user: HackleUserDto,
    val operations: Map<String, Any>,
)

internal class HackleUserDto(
    val identifiers: Map<String, String>,
    val userProperties: Map<String, Any>,
    val hackleProperties: Map<String, Any>,
)

internal class WorkspaceEvaluateRequestDto(
    val scope: String, // ALL, SPECIFIC
    val policy: String, // AUTO, FORCE_FULL
    val context: RemoteEvaluateContextDto,
    val entities: List<EvaluateEntityDto>,
    val current: WorkspaceEvaluationMetadataDto?,
)

internal class WorkspaceEvaluateResponseDto(
    val status: String,
    val evaluation: WorkspaceEvaluationDto?,
    val deleted: List<EntityDto>,
)
