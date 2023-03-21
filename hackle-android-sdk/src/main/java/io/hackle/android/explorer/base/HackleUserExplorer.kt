package io.hackle.android.explorer.base

import io.hackle.android.explorer.storage.HackleUserManualOverrideStorage
import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.client.HackleInternalClient
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.user.HackleUser

internal class HackleUserExplorer(
    private val client: HackleInternalClient,
    private val userManager: UserManager,
    private val hackleUserResolver: HackleUserResolver,
    private val abTestOverrideStorage: HackleUserManualOverrideStorage,
    private val featureFlagOverrideStorage: HackleUserManualOverrideStorage,
) {

    fun currentUser(): HackleUser {
        val currentUser = userManager.currentUser
        return hackleUserResolver.resolve(currentUser)
    }

    fun getAbTestDecisions(): List<Pair<Experiment, Decision>> {
        return client.experiments(currentUser()).toList()
    }

    fun getAbTestOverrides(): Map<Long, Long> {
        return abTestOverrideStorage.getAll()
    }

    fun setAbTestOverride(experiment: Experiment, variationId: Long) {
        abTestOverrideStorage.set(experiment, variationId)
        increment(AB_TEST, "set")
    }

    fun resetAbTestOverride(experiment: Experiment) {
        abTestOverrideStorage.remove(experiment)
        increment(AB_TEST, "reset")
    }

    fun resetAllAbTestOverride() {
        abTestOverrideStorage.clear()
        increment(AB_TEST, "reset.all")
    }

    fun getFeatureFlagDecisions(): List<Pair<Experiment, FeatureFlagDecision>> {
        return client.featureFlags(currentUser()).toList()
    }

    fun getFeatureFlagOverrides(): Map<Long, Long> {
        return featureFlagOverrideStorage.getAll()
    }

    fun setFeatureFlagOverride(experiment: Experiment, variationId: Long) {
        featureFlagOverrideStorage.set(experiment, variationId)
        increment(FEATURE_FLAG, "set")
    }

    fun resetFeatureFlagOverride(experiment: Experiment) {
        featureFlagOverrideStorage.remove(experiment)
        increment(FEATURE_FLAG, "reset")
    }

    fun resetAllFeatureFlagOverride() {
        featureFlagOverrideStorage.clear()
        increment(FEATURE_FLAG, "reset.all")
    }

    private fun increment(experimentType: Experiment.Type, operation: String) {
        val tags = mapOf(
            "experiment.type" to experimentType.name,
            "operation" to operation
        )
        Metrics.counter("experiment.manual.override", tags).increment()
    }
}
