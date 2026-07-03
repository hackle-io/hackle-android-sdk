package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.workspace.config.parseEnumOrNull
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDto
import io.hackle.android.internal.workspace.evaluation.model.toResult
import io.hackle.android.internal.workspace.evaluation.model.toResultOrNull
import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.core.model.Entity
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.model.ServiceType
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.entity.ExperimentRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageEligibilityRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.RemoteConfigParameterRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.RemoteEvaluateResult

internal class DefaultWorkspaceEvaluation(
    // Metadata
    override val id: Long,
    override val environmentId: Long,
    override val evaluatedAt: Long,
    override val lastModified: String?,

    // Entity
    override val experiments: List<ExperimentRemoteEvaluateResult>,
    override val featureFlags: List<ExperimentRemoteEvaluateResult>,
    override val remoteConfigParameters: List<RemoteConfigParameterRemoteEvaluateResult>,
    override val inAppMessages: List<InAppMessageEligibilityRemoteEvaluateResult>,
) : WorkspaceEvaluation, WorkspaceConfig.Metadata {

    override val metadata: WorkspaceConfig.Metadata get() = this

    private val _experiments = experiments.associateBy { it.key }
    private val _featureFlags = featureFlags.associateBy { it.key }
    private val _remoteConfigParameters = remoteConfigParameters.associateBy { it.key }
    private val _inAppMessage = inAppMessages.associateBy { it.key }

    override fun getExperimentOrNull(experimentKey: Long): ExperimentRemoteEvaluateResult? {
        return _experiments[experimentKey]
    }

    override fun getFeatureFlagOrNull(featureKey: Long): ExperimentRemoteEvaluateResult? {
        return _featureFlags[featureKey]
    }

    override fun getRemoteConfigParameterOrNull(parameterKey: String): RemoteConfigParameterRemoteEvaluateResult? {
        return _remoteConfigParameters[parameterKey]
    }

    override fun getInAppMessageOrNull(inAppMessageKey: Long): InAppMessageEligibilityRemoteEvaluateResult? {
        return _inAppMessage[inAppMessageKey]
    }

    override fun result(entity: Entity): RemoteEvaluateResult? {
        val entities: List<RemoteEvaluateResult> = when (entity.serviceType) {
            ServiceType.AB_TEST -> experiments
            ServiceType.FEATURE_FLAG -> featureFlags
            ServiceType.REMOTE_CONFIG -> remoteConfigParameters
            ServiceType.IN_APP_MESSAGE -> inAppMessages
        }
        return entities.find { it.id == entity.id }
    }

    override fun toProperties(): Map<String, Any> {
        return PropertiesBuilder()
            .add("config_modified_at", lastModified)
            .add("remote_evaluated_at", evaluatedAt)
            .build()
    }

    companion object {
        fun from(dto: WorkspaceEvaluationDto): DefaultWorkspaceEvaluation {

            val experiments = mutableListOf<ExperimentRemoteEvaluateResult>()
            val featureFlags = mutableListOf<ExperimentRemoteEvaluateResult>()
            val remoteConfigParameters = mutableListOf<RemoteConfigParameterRemoteEvaluateResult>()
            val inAppMessages = mutableListOf<InAppMessageEligibilityRemoteEvaluateResult>()

            for (result in dto.results) {
                val serviceType = parseEnumOrNull<ServiceType>(result.type) ?: continue
                when (serviceType) {
                    ServiceType.AB_TEST -> result.experiment?.toResult(AB_TEST)?.let(experiments::add)
                    ServiceType.FEATURE_FLAG -> result.featureFlag?.toResult(FEATURE_FLAG)?.let(featureFlags::add)
                    ServiceType.REMOTE_CONFIG -> result.remoteConfig?.toResultOrNull()?.let(remoteConfigParameters::add)
                    ServiceType.IN_APP_MESSAGE -> result.inAppMessage?.toResultOrNull()?.let(inAppMessages::add)
                }
            }
            return DefaultWorkspaceEvaluation(
                id = dto.workspace.id,
                environmentId = dto.workspace.environment.id,
                evaluatedAt = dto.metadata.evaluatedAt,
                lastModified = dto.metadata.config.lastModified,
                experiments = experiments,
                featureFlags = featureFlags,
                remoteConfigParameters = remoteConfigParameters,
                inAppMessages = inAppMessages
            )
        }
    }
}