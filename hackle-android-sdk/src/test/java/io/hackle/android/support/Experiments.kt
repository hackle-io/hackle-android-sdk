package io.hackle.android.support

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.Evaluation
import io.hackle.sdk.core.evaluation.EvaluationPhase
import io.hackle.sdk.core.evaluation.service.experiment.ExperimentEvaluateResponse
import io.hackle.sdk.core.evaluation.service.experiment.ExperimentEvaluateResult
import io.hackle.sdk.core.evaluation.service.experiment.ExperimentEvaluation
import io.hackle.sdk.core.evaluation.service.experiment.mode.local.ExperimentLocalEvaluateRequest
import io.hackle.sdk.core.evaluation.service.experiment.mode.remote.ExperimentRemoteEvaluateRequest
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.entity.ExperimentConfig
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.entity.ExperimentRemoteEvaluateResult

internal object Experiments {

    fun config(
        id: Long = 1,
        key: Long = 1,
        type: Experiment.Type = Experiment.Type.AB_TEST,
        identifierType: String = "\$id",
        status: Experiment.Status = Experiment.Status.RUNNING,
        version: Int = 1,
        executionVersion: Int = 1,
        order: Long = id,
        name: String? = null,
        containerId: Long? = null,
        variations: List<Variation> = variations("A", "B"),
        userOverrides: Map<String, Long> = emptyMap(),
        segmentOverrides: List<TargetRule> = emptyList(),
        targetAudiences: List<Target> = emptyList(),
        targetRules: List<TargetRule> = emptyList(),
        defaultRule: Action = Action.Bucket(1),
        winnerVariationKey: String? = null,
    ): ExperimentConfig {
        return ExperimentConfig(
            id = id,
            key = key,
            version = version,
            status = status,
            order = order,
            type = type,
            executionVersion = executionVersion,
            name = name,
            identifierType = identifierType,
            variations = variations,
            userOverrides = userOverrides,
            segmentOverrides = segmentOverrides,
            targetAudiences = targetAudiences,
            targetRules = targetRules,
            defaultRule = defaultRule,
            containerId = containerId,
            winnerVariationId = winnerVariationKey?.let { winnerKey ->
                variations.first { it.key == winnerKey }.id
            }
        )
    }

    fun remoteResult(
        id: Long = 1,
        key: Long = 1,
        type: Experiment.Type = Experiment.Type.AB_TEST,
        status: Experiment.Status = Experiment.Status.RUNNING,
        version: Int = 1,
        executionVersion: Int = 1,
        order: Long = id,
        variation: Variation = variation(),
        reason: DecisionReason = DecisionReason.TRAFFIC_ALLOCATED,
        references: List<Entity> = emptyList(),
    ): ExperimentRemoteEvaluateResult {
        return ExperimentRemoteEvaluateResult(
            id = id,
            key = key,
            version = version,
            status = status,
            order = order,
            type = type,
            executionVersion = executionVersion,
            variation = variation,
            reason = reason,
            references = references
        )
    }

    fun localRequest(
        workspace: WorkspaceConfig = Workspaces.config(),
        experiment: ExperimentConfig = config(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        phase: EvaluationPhase = EvaluationPhase.RUNTIME,
        record: Boolean = true,
    ): ExperimentLocalEvaluateRequest {
        return ExperimentLocalEvaluateRequest.of(
            workspace = workspace,
            entity = experiment,
            user = user,
            phase = phase,
            record = record
        )
    }

    fun remoteRequest(
        workspace: WorkspaceEvaluation = Workspaces.evaluation(),
        experiment: ExperimentRemoteEvaluateResult = remoteResult(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        record: Boolean = true,
    ): ExperimentRemoteEvaluateRequest {
        return ExperimentRemoteEvaluateRequest.of(
            workspace = workspace,
            entity = experiment,
            user = user,
            record = record
        )
    }

    fun result(
        reason: DecisionReason = DecisionReason.TRAFFIC_ALLOCATED,
        variation: Variation = variation(),
    ): ExperimentEvaluateResult {
        return ExperimentEvaluateResult.of(reason, variation)
    }

    fun evaluation(
        entity: Experiment = config(),
        result: ExperimentEvaluateResult = result(),
    ): ExperimentEvaluation {
        return ExperimentEvaluation(entity = entity, result = result)
    }

    fun response(
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        workspace: Workspace = Workspaces.config(),
        evaluation: ExperimentEvaluation = evaluation(),
        references: List<Evaluation> = emptyList(),
    ): ExperimentEvaluateResponse {
        return ExperimentEvaluateResponse(
            user = user,
            workspace = workspace,
            evaluation = evaluation,
            references = references
        )
    }

    fun variation(
        id: Long = 1,
        key: String = "A",
        isDropped: Boolean = false,
        parameterConfiguration: ParameterConfiguration? = null,
    ): Variation {
        return Variation(id, key, isDropped, parameterConfiguration)
    }

    fun variations(vararg keys: String): List<Variation> {
        return keys.mapIndexed { index, key -> variation(id = index + 1L, key = key) }
    }
}
