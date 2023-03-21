package io.hackle.android.explorer.activity.experiment.ab

import android.content.Context
import android.view.ViewGroup
import io.hackle.android.explorer.base.HackleUserExplorer
import io.hackle.android.explorer.activity.experiment.ab.viewholder.AbTestItem
import io.hackle.android.explorer.activity.experiment.ab.viewholder.AbTestViewHolder
import io.hackle.android.explorer.activity.experiment.listener.OnOverrideResetListener
import io.hackle.android.explorer.activity.experiment.listener.OnOverrideSetListener
import io.hackle.android.explorer.base.ListAdapter
import io.hackle.android.internal.task.TaskExecutors.runOnBackground
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Variation

internal class AbTestAdapter(
    private val context: Context,
    private val explorer: HackleUserExplorer,
) : ListAdapter<AbTestItem, AbTestViewHolder>() {

    private val overrideSetListener: OnOverrideSetListener
    private val overrideResetListener: OnOverrideResetListener

    init {
        overrideSetListener = AbTestOverrideListener()
        overrideResetListener = AbTestOverrideResetListener()
        fetchAndUpdate()
    }

    override fun createViewHolder(parent: ViewGroup?): AbTestViewHolder {
        return AbTestViewHolder.create(context, overrideSetListener, overrideResetListener, parent)
    }

    fun fetchAndUpdate() {
        runOnBackground {
            val decisions = explorer.getAbTestDecisions()
            val overrides = explorer.getAbTestOverrides()
            val items = AbTestItem.of(decisions, overrides)
            runOnUiThread {
                update(items)
            }
        }
    }

    private inner class AbTestOverrideListener : OnOverrideSetListener {
        override fun onOverrideSet(experiment: Experiment, variation: Variation) {
            explorer.setAbTestOverride(experiment, variation.id)
            fetchAndUpdate()
        }
    }

    private inner class AbTestOverrideResetListener : OnOverrideResetListener {
        override fun onOverrideReset(experiment: Experiment) {
            explorer.resetAbTestOverride(experiment)
            fetchAndUpdate()
        }
    }
}
