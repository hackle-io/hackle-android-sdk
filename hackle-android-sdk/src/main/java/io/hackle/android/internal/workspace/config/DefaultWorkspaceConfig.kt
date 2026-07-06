package io.hackle.android.internal.workspace.config

import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.core.model.Bucket
import io.hackle.sdk.core.model.Container
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.model.Segment
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.entity.ExperimentConfig
import io.hackle.sdk.core.workspace.config.entity.InAppMessageConfig
import io.hackle.sdk.core.workspace.config.entity.RemoteConfigParameterConfig

internal class DefaultWorkspaceConfig(
    // Metadata
    override val id: Long,
    override val environmentId: Long,
    override val modifiedAt: String?,

    // Entity
    override val experiments: List<ExperimentConfig>,
    override val featureFlags: List<ExperimentConfig>,
    override val remoteConfigParameters: List<RemoteConfigParameterConfig>,
    override val inAppMessages: List<InAppMessageConfig>,

    // Component
    private val buckets: List<Bucket>,
    private val segments: List<Segment>,
    private val containers: List<Container>,
) : WorkspaceConfig, WorkspaceConfig.Metadata {

    override val metadata: WorkspaceConfig.Metadata get() = this

    private val _experiments = experiments.associateBy { it.key }
    private val _featureFlags = featureFlags.associateBy { it.key }
    private val _remoteConfigParameters = remoteConfigParameters.associateBy { it.key }
    private val _inAppMessage = inAppMessages.associateBy { it.key }
    private val _buckets = buckets.associateBy { it.id }
    private val _segments = segments.associateBy { it.key }
    private val _containers = containers.associateBy { it.id }

    override fun getExperimentOrNull(experimentKey: Long): ExperimentConfig? {
        return _experiments[experimentKey]
    }

    override fun getFeatureFlagOrNull(featureKey: Long): ExperimentConfig? {
        return _featureFlags[featureKey]
    }

    override fun getRemoteConfigParameterOrNull(parameterKey: String): RemoteConfigParameterConfig? {
        return _remoteConfigParameters[parameterKey]
    }

    override fun getInAppMessageOrNull(inAppMessageKey: Long): InAppMessageConfig? {
        return _inAppMessage[inAppMessageKey]
    }

    override fun getBucketOrNull(bucketId: Long): Bucket? {
        return _buckets[bucketId]
    }

    override fun getSegmentOrNull(segmentKey: String): Segment? {
        return _segments[segmentKey]
    }

    override fun getContainerOrNull(containerId: Long): Container? {
        return _containers[containerId]
    }

    override fun toProperties(): Map<String, Any> {
        return PropertiesBuilder()
            .add("config_modified_at", modifiedAt)
            .build()
    }

    companion object {
        fun from(dto: WorkspaceConfigDto, modifiedAt: String?): WorkspaceConfig {

            val configurations = dto.parameterConfigurations.asSequence()
                .map { it.toParameterConfiguration() }
                .associateBy { it.id }

            val experiments = dto.experiments.mapNotNull { it.toExperimentOrNull(AB_TEST, configurations) }
            val featureFlags = dto.featureFlags.mapNotNull { it.toExperimentOrNull(FEATURE_FLAG, configurations) }
            val remoteConfigParameters = dto.remoteConfigParameters.mapNotNull { it.toRemoteConfigParameterOrNull() }
            val inAppMessages = dto.inAppMessages.mapNotNull { it.toInAppMessageOrNull() }

            val buckets = dto.buckets.map { it.toBucket() }
            val segments = dto.segments.mapNotNull { it.toSegmentOrNull() }
            val containers = dto.containers.map { it.toContainer() }

            return DefaultWorkspaceConfig(
                id = dto.workspace.id,
                environmentId = dto.workspace.environment.id,
                modifiedAt = modifiedAt,
                experiments = experiments,
                featureFlags = featureFlags,
                remoteConfigParameters = remoteConfigParameters,
                inAppMessages = inAppMessages,
                buckets = buckets,
                segments = segments,
                containers = containers,
            )
        }
    }
}
