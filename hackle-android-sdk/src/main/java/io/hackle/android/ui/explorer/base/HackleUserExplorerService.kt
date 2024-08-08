package io.hackle.android.ui.explorer.base

import io.hackle.android.internal.push.token.PushTokenManager
import io.hackle.android.internal.user.UserManager
import io.hackle.android.ui.explorer.storage.HackleUserManualOverrideStorage
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.user.HackleUser

internal class HackleUserExplorerService(
    private val core: HackleCore,
    private val userManager: UserManager,
    private val abTestOverrideStorage: HackleUserManualOverrideStorage,
    private val featureFlagOverrideStorage: HackleUserManualOverrideStorage,
    private val pushTokenManager: PushTokenManager
) {

    fun currentUser(): HackleUser {
        return userManager.resolve(null)
    }

    fun registeredPushToken(): String? {
        return pushTokenManager.currentPushToken?.value
    }

    fun getAbTestDecisions(): List<Pair<Experiment, Decision>> {
        return core.experiments(currentUser()).toList()
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
        return core.featureFlags(currentUser()).toList()
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
