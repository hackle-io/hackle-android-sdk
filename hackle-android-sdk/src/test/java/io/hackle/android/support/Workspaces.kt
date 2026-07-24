package io.hackle.android.support

import io.hackle.android.internal.workspace.config.*
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationContext
import io.hackle.android.internal.workspace.evaluation.model.*
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.entity.ExperimentConfig
import io.hackle.sdk.core.workspace.config.entity.InAppMessageConfig
import io.hackle.sdk.core.workspace.config.entity.RemoteConfigParameterConfig
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.entity.ExperimentRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageEligibilityRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.RemoteConfigParameterRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.RemoteEvaluateResult

internal object Workspaces {

    fun configMetadata(
        id: Long = 1,
        environmentId: Long = 1,
        modifiedAt: String? = null,
    ): WorkspaceConfig.Metadata {
        return TestConfigMetadata(id, environmentId, modifiedAt)
    }

    fun config(
        metadata: WorkspaceConfig.Metadata = configMetadata(),
        experiments: List<ExperimentConfig> = emptyList(),
        featureFlags: List<ExperimentConfig> = emptyList(),
        remoteConfigParameters: List<RemoteConfigParameterConfig> = emptyList(),
        inAppMessages: List<InAppMessageConfig> = emptyList(),
        buckets: List<Bucket> = emptyList(),
        segments: List<Segment> = emptyList(),
        containers: List<Container> = emptyList(),
    ): WorkspaceConfig {
        return TestWorkspaceConfig(
            metadata = metadata,
            experiments = experiments,
            featureFlags = featureFlags,
            remoteConfigParameters = remoteConfigParameters,
            inAppMessages = inAppMessages,
            buckets = buckets,
            segments = segments,
            containers = containers
        )
    }

    fun evaluationMetadata(
        id: Long = 1,
        environmentId: Long = 1,
        modifiedAt: String? = null,
        evaluatedAt: Long = 42,
    ): WorkspaceEvaluation.Metadata {
        return TestEvaluationMetadata(id, environmentId, modifiedAt, evaluatedAt)
    }

    fun evaluation(
        metadata: WorkspaceEvaluation.Metadata = evaluationMetadata(),
        experiments: List<ExperimentRemoteEvaluateResult> = emptyList(),
        featureFlags: List<ExperimentRemoteEvaluateResult> = emptyList(),
        remoteConfigParameters: List<RemoteConfigParameterRemoteEvaluateResult> = emptyList(),
        inAppMessages: List<InAppMessageEligibilityRemoteEvaluateResult> = emptyList(),
    ): WorkspaceEvaluation {
        return TestWorkspaceEvaluation(
            metadata = metadata,
            experiments = experiments,
            featureFlags = featureFlags,
            remoteConfigParameters = remoteConfigParameters,
            inAppMessages = inAppMessages
        )
    }

    // ── Android DTO ──

    fun configDto(
        id: Long = 1,
        environmentId: Long = 2,
    ): WorkspaceConfigDto {
        return WorkspaceConfigDto(
            workspace = WorkspaceDto(id = id, environment = EnvironmentDto(id = environmentId)),
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )
    }

    fun configContext(
        dto: WorkspaceConfigDto = configDto(),
        modifiedAt: String? = "modified_at",
    ): WorkspaceConfigContext {
        return WorkspaceConfigContext.of(dto, modifiedAt)
    }

    fun workspaceDto(
        id: Long = 1,
        environmentId: Long = 2,
    ): WorkspaceDto {
        return WorkspaceDto(
            id = id,
            environment = EnvironmentDto(id = environmentId)
        )
    }

    fun evaluationMetadataDto(
        hash: Int = 320,
        evaluatedAt: Long = 42,
        userHash: Int = 0,
        modifiedAt: String = "modified_at",
    ): WorkspaceEvaluationMetadataDto {
        return WorkspaceEvaluationMetadataDto(
            hash = hash,
            evaluatedAt = evaluatedAt,
            user = HackleUserMetadataDto(hash = userHash),
            config = WorkspaceConfigMetadataDto(modifiedAt = modifiedAt)
        )
    }

    fun resultDto(
        type: String = "AB_TEST",
        id: Long = 1,
        hash: Int = id.toInt(),
        experiment: ExperimentEvaluateResultDto? = null,
        featureFlag: ExperimentEvaluateResultDto? = null,
        remoteConfig: RemoteConfigParameterEvaluateResultDto? = null,
        inAppMessage: InAppMessageEligibilityEvaluateResultDto? = null,
    ): EvaluateResultDto {
        return EvaluateResultDto(
            type = type,
            id = id,
            hash = hash,
            experiment = experiment,
            featureFlag = featureFlag,
            remoteConfig = remoteConfig,
            inAppMessage = inAppMessage
        )
    }

    fun experimentResultDto(
        id: Long = 1,
        key: Long = id,
        order: Long = id,
        version: Int = 1,
        executionStatus: String = "RUNNING",
        executionVersion: Int = 1,
        variation: VariationDto = VariationDto(id = 1, key = "A", status = "ACTIVE", parameterConfigurationId = null),
        config: ParameterConfigurationDto? = null,
        reason: String = "TRAFFIC_ALLOCATED",
        references: List<EntityDto> = emptyList(),
    ): ExperimentEvaluateResultDto {
        return ExperimentEvaluateResultDto(
            id = id,
            key = key,
            order = order,
            version = version,
            execution = ExperimentEvaluateResultDto.ExecutionDto(
                status = executionStatus,
                version = executionVersion
            ),
            variation = variation,
            config = config,
            reason = reason,
            references = references
        )
    }

    fun remoteConfigResultDto(
        id: Long = 1,
        key: String = "remote_config",
        valueType: String = "STRING",
        value: RemoteConfigParameterDto.ValueDto? = RemoteConfigParameterDto.ValueDto(id = 1, value = "value"),
        reason: String = "TARGET_RULE_MATCH",
        references: List<EntityDto> = emptyList(),
    ): RemoteConfigParameterEvaluateResultDto {
        return RemoteConfigParameterEvaluateResultDto(
            id = id,
            key = key,
            valueType = valueType,
            value = value,
            reason = reason,
            references = references
        )
    }

    fun messageDto(
        displayType: String = "MODAL",
        layoutType: String = "IMAGE_ONLY",
    ): InAppMessageDto.MessageContextDto.MessageDto {
        return InAppMessageDto.MessageContextDto.MessageDto(
            variationKey = null,
            lang = "ko",
            layout = InAppMessageDto.MessageContextDto.MessageDto.LayoutDto(
                displayType = displayType,
                layoutType = layoutType,
                alignment = null
            ),
            images = emptyList(),
            imageAutoScroll = null,
            text = null,
            buttons = emptyList(),
            background = InAppMessageDto.MessageContextDto.MessageDto.BackgroundDto(color = "#FFFFFF"),
            closeButton = null,
            action = null,
            outerButtons = emptyList(),
            innerButtons = emptyList(),
            html = null
        )
    }

    fun messageContextDto(
        platformTypes: List<String> = listOf("ANDROID"),
        orientations: List<String> = listOf("VERTICAL"),
        messages: List<InAppMessageDto.MessageContextDto.MessageDto> = listOf(messageDto()),
    ): InAppMessageDto.MessageContextDto {
        return InAppMessageDto.MessageContextDto(
            defaultLang = "ko",
            exposure = InAppMessageDto.MessageContextDto.MessageDto.ExposureDto(type = "DEFAULT", key = null),
            platformTypes = platformTypes,
            orientations = orientations,
            messages = messages
        )
    }

    fun inAppMessageResultDto(
        id: Long = 1,
        key: Long = id,
        order: Long = id,
        isEligible: Boolean = true,
        reason: String = "IN_APP_MESSAGE_TARGET",
        evaluateContext: InAppMessageDto.EvaluateContextDto? = null,
        messageContext: InAppMessageDto.MessageContextDto = messageContextDto(),
        layout: InAppMessageLayoutEvaluateResultDto = InAppMessageLayoutEvaluateResultDto(
            message = messageContext.messages.first(),
            reason = reason,
            references = emptyList()
        ),
        references: List<EntityDto> = emptyList(),
    ): InAppMessageEligibilityEvaluateResultDto {
        return InAppMessageEligibilityEvaluateResultDto(
            id = id,
            key = key,
            order = order,
            period = null,
            timetable = null,
            eventTriggerRules = emptyList(),
            eventFrequencyCap = null,
            eventTriggerDelay = null,
            evaluateContext = evaluateContext,
            messageContext = messageContext,
            isEligible = isEligible,
            layout = layout,
            reason = reason,
            references = references
        )
    }

    fun entityDto(
        type: String = "AB_TEST",
        id: Long = 1,
    ): EntityDto {
        return EntityDto(type = type, id = id)
    }

    fun evaluationDto(
        workspace: WorkspaceDto = workspaceDto(),
        metadata: WorkspaceEvaluationMetadataDto = evaluationMetadataDto(),
        results: List<EvaluateResultDto> = emptyList(),
    ): WorkspaceEvaluationDto {
        return WorkspaceEvaluationDto(
            workspace = workspace,
            metadata = metadata,
            results = results
        )
    }

    fun deltaDto(
        metadata: WorkspaceEvaluationMetadataDto = evaluationMetadataDto(),
        changed: List<EvaluateResultDto> = emptyList(),
        deleted: List<EntityDto> = emptyList(),
    ): WorkspaceEvaluationDeltaDto {
        return WorkspaceEvaluationDeltaDto(
            metadata = metadata,
            changed = changed,
            deleted = deleted
        )
    }

    fun entityMetadataDto(
        evaluatedAt: Long = 42,
        modifiedAt: String = "modified_at",
    ): EntityEvaluationMetadataDto {
        return EntityEvaluationMetadataDto(
            evaluatedAt = evaluatedAt,
            config = WorkspaceConfigMetadataDto(modifiedAt = modifiedAt)
        )
    }

    fun entityEvaluationDto(
        workspace: WorkspaceDto = workspaceDto(),
        metadata: EntityEvaluationMetadataDto = entityMetadataDto(),
        results: List<EvaluateResultDto> = emptyList(),
    ): EntityEvaluationDto {
        return EntityEvaluationDto(
            workspace = workspace,
            metadata = metadata,
            results = results
        )
    }

    fun evaluationKey(vararg identifiers: Pair<String, String>): WorkspaceEvaluationContext.Key {
        val ids = if (identifiers.isEmpty()) mapOf("\$id" to "user") else identifiers.toMap()
        return WorkspaceEvaluationContext.Key(Identifiers.from(ids))
    }

    fun evaluationContext(
        key: WorkspaceEvaluationContext.Key = evaluationKey(),
        evaluation: WorkspaceEvaluationDto = evaluationDto(),
        fullEvaluatedAt: Long = 42,
    ): WorkspaceEvaluationContext {
        return WorkspaceEvaluationContext.of(
            key = key,
            dto = evaluation,
            fullEvaluatedAt = fullEvaluatedAt
        )
    }

    private class TestConfigMetadata(
        override val id: Long,
        override val environmentId: Long,
        override val modifiedAt: String?,
    ) : WorkspaceConfig.Metadata

    private class TestEvaluationMetadata(
        override val id: Long,
        override val environmentId: Long,
        override val modifiedAt: String?,
        override val evaluatedAt: Long,
    ) : WorkspaceEvaluation.Metadata

    private class TestWorkspaceConfig(
        override val metadata: WorkspaceConfig.Metadata,
        override val experiments: List<ExperimentConfig>,
        override val featureFlags: List<ExperimentConfig>,
        override val remoteConfigParameters: List<RemoteConfigParameterConfig>,
        override val inAppMessages: List<InAppMessageConfig>,
        private val buckets: List<Bucket>,
        private val segments: List<Segment>,
        private val containers: List<Container>,
    ) : WorkspaceConfig {

        override fun getExperimentOrNull(experimentKey: Long): ExperimentConfig? {
            return experiments.find { it.key == experimentKey }
        }

        override fun getFeatureFlagOrNull(featureKey: Long): ExperimentConfig? {
            return featureFlags.find { it.key == featureKey }
        }

        override fun getRemoteConfigParameterOrNull(parameterKey: String): RemoteConfigParameterConfig? {
            return remoteConfigParameters.find { it.key == parameterKey }
        }

        override fun getInAppMessageOrNull(inAppMessageKey: Long): InAppMessageConfig? {
            return inAppMessages.find { it.key == inAppMessageKey }
        }

        override fun getBucketOrNull(bucketId: Long): Bucket? {
            return buckets.find { it.id == bucketId }
        }

        override fun getSegmentOrNull(segmentKey: String): Segment? {
            return segments.find { it.key == segmentKey }
        }

        override fun getContainerOrNull(containerId: Long): Container? {
            return containers.find { it.id == containerId }
        }

        override fun toProperties(): Map<String, Any> {
            val modifiedAt = metadata.modifiedAt ?: return emptyMap()
            return mapOf("config_modified_at" to modifiedAt)
        }
    }

    private class TestWorkspaceEvaluation(
        override val metadata: WorkspaceEvaluation.Metadata,
        override val experiments: List<ExperimentRemoteEvaluateResult>,
        override val featureFlags: List<ExperimentRemoteEvaluateResult>,
        override val remoteConfigParameters: List<RemoteConfigParameterRemoteEvaluateResult>,
        override val inAppMessages: List<InAppMessageEligibilityRemoteEvaluateResult>,
    ) : WorkspaceEvaluation {

        private val results: List<RemoteEvaluateResult> =
            experiments + featureFlags + remoteConfigParameters + inAppMessages

        override fun getExperimentOrNull(experimentKey: Long): ExperimentRemoteEvaluateResult? {
            return experiments.find { it.key == experimentKey }
        }

        override fun getFeatureFlagOrNull(featureKey: Long): ExperimentRemoteEvaluateResult? {
            return featureFlags.find { it.key == featureKey }
        }

        override fun getRemoteConfigParameterOrNull(parameterKey: String): RemoteConfigParameterRemoteEvaluateResult? {
            return remoteConfigParameters.find { it.key == parameterKey }
        }

        override fun getInAppMessageOrNull(inAppMessageKey: Long): InAppMessageEligibilityRemoteEvaluateResult? {
            return inAppMessages.find { it.key == inAppMessageKey }
        }

        override fun result(entity: Entity): RemoteEvaluateResult? {
            return results.find { it == entity }
        }

        override fun toProperties(): Map<String, Any> {
            val modifiedAt = metadata.modifiedAt ?: return emptyMap()
            return mapOf("config_modified_at" to modifiedAt)
        }
    }
}
