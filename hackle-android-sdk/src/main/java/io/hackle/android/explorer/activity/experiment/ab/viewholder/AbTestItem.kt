package io.hackle.android.explorer.activity.experiment.ab.viewholder

import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Variation

internal class AbTestItem(
    val experiment: Experiment,
    val decision: Decision,
    val overriddenVariation: Variation?,
) : Comparable<AbTestItem> {

    override fun compareTo(other: AbTestItem): Int {
        return this.experiment.key.compareTo(other.experiment.key)
    }

    companion object {
        fun of(
            decisions: List<Pair<Experiment, Decision>>,
            overrides: Map<Long, Long>,
        ): List<AbTestItem> {
            return decisions.asSequence()
                .map { (experiment, decision) ->
                    val overriddenVariationId = overrides[experiment.id]
                    val overriddenVariation =
                        overriddenVariationId?.let { experiment.getVariationOrNull(it) }
                    AbTestItem(experiment, decision, overriddenVariation)
                }
                .sortedDescending()
                .toList()
        }
    }
}
