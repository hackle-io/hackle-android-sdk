package io.hackle.android.support

import io.hackle.sdk.core.evaluation.service.experiment.match.ExperimentManualOverrideStorage
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.match.InAppMessageHiddenStorage
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.match.InAppMessageImpression
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.match.InAppMessageImpressionStorage
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.Variation
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.WorkspaceConfigFetcher
import io.hackle.sdk.core.workspace.config.entity.ExperimentConfig
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluationFetcher

internal class InMemoryExperimentManualOverrideStorage : ExperimentManualOverrideStorage {

    private val overrides = mutableMapOf<Long, Variation>()

    override fun get(experiment: ExperimentConfig, user: HackleUser): Variation? {
        return overrides[experiment.id]
    }

    fun put(experiment: ExperimentConfig, variation: Variation) {
        overrides[experiment.id] = variation
    }

    fun remove(experiment: ExperimentConfig) {
        overrides.remove(experiment.id)
    }
}

internal class InMemoryInAppMessageImpressionStorage : InAppMessageImpressionStorage {

    private val impressions = mutableMapOf<Long, List<InAppMessageImpression>>()

    override fun get(inAppMessage: InAppMessage): List<InAppMessageImpression> {
        return impressions[inAppMessage.id] ?: emptyList()
    }

    override fun set(inAppMessage: InAppMessage, impressions: List<InAppMessageImpression>) {
        this.impressions[inAppMessage.id] = impressions
    }
}

internal class InMemoryInAppMessageHiddenStorage : InAppMessageHiddenStorage {

    private val hidden = mutableMapOf<Long, Long>()

    override fun exist(inAppMessage: InAppMessage, now: Long): Boolean {
        val expireAt = hidden[inAppMessage.id] ?: return false
        if (now <= expireAt) {
            return true
        }
        hidden.remove(inAppMessage.id)
        return false
    }

    override fun put(inAppMessage: InAppMessage, expireAt: Long) {
        hidden[inAppMessage.id] = expireAt
    }
}

internal class FixedWorkspaceConfigFetcher(
    private val workspace: WorkspaceConfig? = Workspaces.config(),
) : WorkspaceConfigFetcher {

    override fun metadata(): Workspace.Metadata? {
        return workspace?.metadata
    }

    override fun workspace(user: HackleUser): WorkspaceConfig? {
        return workspace
    }
}

internal class FixedWorkspaceEvaluationFetcher(
    private val workspace: WorkspaceEvaluation? = Workspaces.evaluation(),
) : WorkspaceEvaluationFetcher {

    override fun metadata(): Workspace.Metadata? {
        return workspace?.metadata
    }

    override fun workspace(user: HackleUser): WorkspaceEvaluation? {
        return workspace
    }
}
