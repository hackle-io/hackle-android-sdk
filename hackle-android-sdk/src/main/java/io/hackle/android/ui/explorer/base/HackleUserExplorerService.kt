package io.hackle.android.ui.explorer.base

import io.hackle.android.internal.bridge.model.toDto
import io.hackle.android.internal.devtools.DevToolsApi
import io.hackle.android.internal.devtools.OverrideRequestDto
import io.hackle.android.internal.push.token.PushTokenManager
import io.hackle.android.internal.task.TaskExecutors.runOnBackground
import io.hackle.android.internal.user.UserManager
import io.hackle.android.ui.explorer.storage.HackleUserManualOverrideStorage
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG
import io.hackle.sdk.core.model.Variation
import io.hackle.sdk.core.user.HackleUser

internal class HackleUserExplorerService(
    private val core: HackleCore,
    private val userManager: UserManager,
    private val abTestOverrideStorage: HackleUserManualOverrideStorage,
    private val featureFlagOverrideStorage: HackleUserManualOverrideStorage,
    private val pushTokenManager: PushTokenManager,
    private val devToolsApi: DevToolsApi,
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

    fun setAbTestOverride(experiment: Experiment, variation: Variation) {
        abTestOverrideStorage.set(experiment, variation.id)
        increment(AB_TEST, "set")
        runOnBackground {
            try {
                val requestDto = createOverrideRequest(variation)
                devToolsApi.addExperimentOverrides(experiment.key, requestDto)
            } catch (e: Exception) {
                log.error { "Failed to add experiment overrides to server: $e" }
            }
        }
    }

    fun resetAbTestOverride(experiment: Experiment, variation: Variation) {
        abTestOverrideStorage.remove(experiment)
        increment(AB_TEST, "reset")
        runOnBackground {
            try {
                val requestDto = createOverrideRequest(variation)
                devToolsApi.removeExperimentOverrides(experiment.key, requestDto)
            } catch (e: Exception) {
                log.error { "Failed to remove experiment overrides to server: $e" }
            }
        }
    }

    fun resetAllAbTestOverride() {
        abTestOverrideStorage.clear()
        increment(AB_TEST, "reset.all")
        runOnBackground {
            try {
                val requestDto = createOverrideRequest()
                devToolsApi.removeAllExperimentOverrides(requestDto)
            } catch (e: Exception) {
                log.error { "Failed to remove all experiment overrides to server: $e" }
            }
        }
    }

    fun getFeatureFlagDecisions(): List<Pair<Experiment, FeatureFlagDecision>> {
        return core.featureFlags(currentUser()).toList()
    }

    fun getFeatureFlagOverrides(): Map<Long, Long> {
        return featureFlagOverrideStorage.getAll()
    }

    fun setFeatureFlagOverride(experiment: Experiment, variation: Variation) {
        featureFlagOverrideStorage.set(experiment, variation.id)
        increment(FEATURE_FLAG, "set")
        runOnBackground {
            try {
                val requestDto = createOverrideRequest(variation)
                devToolsApi.addFeatureFlagOverrides(experiment.key, requestDto)
            } catch (e: Exception) {
                log.error { "Failed to add feature flag overrides to server: $e" }
            }
        }
    }

    fun resetFeatureFlagOverride(experiment: Experiment, variation: Variation) {
        featureFlagOverrideStorage.remove(experiment)
        increment(FEATURE_FLAG, "reset")
        runOnBackground {
            try {
                val requestDto = createOverrideRequest(variation)
                devToolsApi.removeFeatureFlagOverrides(experiment.key, requestDto)
            } catch (e: Exception) {
                log.error { "Failed to remove feature flag overrides to server: $e" }
            }
        }
    }

    fun resetAllFeatureFlagOverride() {
        featureFlagOverrideStorage.clear()
        increment(FEATURE_FLAG, "reset.all")
        runOnBackground {
            try {
                val requestDto = createOverrideRequest()
                devToolsApi.removeAllFeatureFlagOverrides(requestDto)
            } catch (e: Exception) {
                log.error { "Failed to remove all feature flag overrides to server: $e" }
            }
        }
    }

    private fun createOverrideRequest(variation: Variation? = null): OverrideRequestDto {
        val hackleUser = currentUser()
        val userDto = userManager.currentUser.toBuilder()
            .id(hackleUser.id)
            .deviceId(hackleUser.deviceId)
            .userId(hackleUser.userId)
            .properties(hackleUser.properties)
            .build()
            .toDto()
        return OverrideRequestDto(
            user = userDto,
            variation = variation?.key
        )
    }

    private fun increment(experimentType: Experiment.Type, operation: String) {
        val tags = mapOf(
            "experiment.type" to experimentType.name,
            "operation" to operation
        )
        Metrics.counter("experiment.manual.override", tags).increment()
    }

    companion object {
        private val log = Logger<HackleUserExplorerService>()
    }
}
