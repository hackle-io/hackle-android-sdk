package io.hackle.android.ui.explorer.activity.experiment.ff.viewholder

import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Variation

internal class FeatureFlagItem(
    val experiment: Experiment,
    val decision: FeatureFlagDecision,
    val overriddenVariation: Variation?,
) : Comparable<FeatureFlagItem> {
    override fun compareTo(other: FeatureFlagItem): Int {
        return this.experiment.key.compareTo(other.experiment.key)
    }

    companion object {
        fun of(
            decisions: List<Pair<Experiment, FeatureFlagDecision>>,
            overrides: Map<Long, Long>,
        ): List<FeatureFlagItem> {
            return decisions.asSequence()
                .map { (experiment, decision) ->
                    val overriddenVariationId = overrides[experiment.id]
                    val overriddenVariation =
                        overriddenVariationId?.let { experiment.getVariationOrNull(it) }
                    FeatureFlagItem(experiment, decision, overriddenVariation)
                }
                .sortedDescending()
                .toList()
        }
    }
}
