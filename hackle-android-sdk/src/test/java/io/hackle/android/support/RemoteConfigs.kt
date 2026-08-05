package io.hackle.android.support

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.Evaluation
import io.hackle.sdk.core.evaluation.EvaluationPhase
import io.hackle.sdk.core.evaluation.service.remoteconfig.RemoteConfigEvaluateResponse
import io.hackle.sdk.core.evaluation.service.remoteconfig.RemoteConfigEvaluateResult
import io.hackle.sdk.core.evaluation.service.remoteconfig.RemoteConfigEvaluation
import io.hackle.sdk.core.evaluation.service.remoteconfig.mode.local.RemoteConfigLocalEvaluateRequest
import io.hackle.sdk.core.evaluation.service.remoteconfig.mode.remote.RemoteConfigRemoteEvaluateRequest
import io.hackle.sdk.core.model.Entity
import io.hackle.sdk.core.model.RemoteConfigParameter
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.ValueType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.entity.RemoteConfigParameterConfig
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.entity.RemoteConfigParameterRemoteEvaluateResult

internal object RemoteConfigs {

    fun config(
        id: Long = 1,
        key: String = "remote_config_parameter",
        type: ValueType = ValueType.STRING,
        identifierType: String = "\$id",
        targetRules: List<RemoteConfigParameter.TargetRule> = emptyList(),
        defaultValue: RemoteConfigParameter.Value = value(),
    ): RemoteConfigParameterConfig {
        return RemoteConfigParameterConfig(
            id = id,
            key = key,
            type = type,
            identifierType = identifierType,
            targetRules = targetRules,
            defaultValue = defaultValue
        )
    }

    fun remoteResult(
        id: Long = 1,
        key: String = "remote_config_parameter",
        type: ValueType = ValueType.STRING,
        value: RemoteConfigParameter.Value? = value(),
        reason: DecisionReason = DecisionReason.DEFAULT_RULE,
        references: List<Entity> = emptyList(),
    ): RemoteConfigParameterRemoteEvaluateResult {
        return RemoteConfigParameterRemoteEvaluateResult(
            id = id,
            key = key,
            type = type,
            value = value,
            reason = reason,
            references = references
        )
    }

    fun localRequest(
        workspace: WorkspaceConfig = Workspaces.config(),
        parameter: RemoteConfigParameterConfig = config(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        requiredType: ValueType = ValueType.STRING,
        phase: EvaluationPhase = EvaluationPhase.RUNTIME,
        record: Boolean = true,
    ): RemoteConfigLocalEvaluateRequest {
        return RemoteConfigLocalEvaluateRequest.of(
            workspace = workspace,
            entity = parameter,
            user = user,
            requiredType = requiredType,
            phase = phase,
            record = record
        )
    }

    fun remoteRequest(
        workspace: WorkspaceEvaluation = Workspaces.evaluation(),
        parameter: RemoteConfigParameterRemoteEvaluateResult = remoteResult(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        requiredType: ValueType = ValueType.STRING,
        record: Boolean = true,
    ): RemoteConfigRemoteEvaluateRequest {
        return RemoteConfigRemoteEvaluateRequest.of(
            workspace = workspace,
            entity = parameter,
            user = user,
            requiredType = requiredType,
            record = record
        )
    }

    fun result(
        reason: DecisionReason = DecisionReason.DEFAULT_RULE,
        value: RemoteConfigParameter.Value? = value(),
    ): RemoteConfigEvaluateResult {
        return RemoteConfigEvaluateResult.of(reason, value)
    }

    fun evaluation(
        parameter: RemoteConfigParameter = config(),
        result: RemoteConfigEvaluateResult = result(),
    ): RemoteConfigEvaluation {
        return RemoteConfigEvaluation(entity = parameter, result = result)
    }

    fun response(
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        workspace: Workspace = Workspaces.config(),
        evaluation: RemoteConfigEvaluation = evaluation(),
        references: List<Evaluation> = emptyList(),
    ): RemoteConfigEvaluateResponse {
        return RemoteConfigEvaluateResponse(
            user = user,
            workspace = workspace,
            evaluation = evaluation,
            references = references
        )
    }

    fun value(
        id: Long = 1,
        rawValue: Any = "value",
    ): RemoteConfigParameter.Value {
        return RemoteConfigParameter.Value(id, rawValue)
    }

    fun targetRule(
        key: String = "target_rule",
        name: String = "target_rule",
        target: Target = Targets.create(),
        bucketId: Long = 1,
        value: RemoteConfigParameter.Value = value(),
    ): RemoteConfigParameter.TargetRule {
        return RemoteConfigParameter.TargetRule(
            key = key,
            name = name,
            target = target,
            bucketId = bucketId,
            value = value
        )
    }
}
