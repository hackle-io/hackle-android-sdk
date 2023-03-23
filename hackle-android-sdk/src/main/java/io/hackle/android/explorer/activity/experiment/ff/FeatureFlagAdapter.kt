package io.hackle.android.explorer.activity.experiment.ff

import android.content.Context
import android.view.ViewGroup
import io.hackle.android.explorer.base.HackleUserExplorer
import io.hackle.android.explorer.activity.experiment.ff.viewholder.FeatureFlagItem
import io.hackle.android.explorer.activity.experiment.ff.viewholder.FeatureFlagViewHolder
import io.hackle.android.explorer.activity.experiment.listener.OnOverrideResetListener
import io.hackle.android.explorer.activity.experiment.listener.OnOverrideSetListener
import io.hackle.android.explorer.base.ListAdapter
import io.hackle.android.internal.task.TaskExecutors.runOnBackground
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Variation

internal class FeatureFlagAdapter(
    private val context: Context,
    private val explorer: HackleUserExplorer,
) : ListAdapter<FeatureFlagItem, FeatureFlagViewHolder>() {

    private val overrideSetListener: OnOverrideSetListener
    private val overrideResetListener: OnOverrideResetListener

    init {
        overrideSetListener = FeatureFlagOverrideListener()
        overrideResetListener = FeatureFlagOverrideResetListener()
        fetchAndUpdate()
    }

    override fun createViewHolder(parent: ViewGroup?): FeatureFlagViewHolder {
        return FeatureFlagViewHolder.create(
            context, overrideSetListener, overrideResetListener, parent)
    }

    fun fetchAndUpdate() {
        runOnBackground {
            val decisions = explorer.getFeatureFlagDecisions()
            val overrides = explorer.getFeatureFlagOverrides()
            val items = FeatureFlagItem.of(decisions, overrides)
            runOnUiThread {
                update(items)
            }
        }
    }

    private inner class FeatureFlagOverrideListener : OnOverrideSetListener {
        override fun onOverrideSet(experiment: Experiment, variation: Variation) {
            explorer.setFeatureFlagOverride(experiment, variation.id)
            fetchAndUpdate()
        }
    }

    private inner class FeatureFlagOverrideResetListener : OnOverrideResetListener {
        override fun onOverrideReset(experiment: Experiment) {
            explorer.resetFeatureFlagOverride(experiment)
            fetchAndUpdate()
        }
    }
}
