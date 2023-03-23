package io.hackle.android.explorer.activity.experiment.listener

import io.hackle.sdk.core.model.Experiment

internal interface OnOverrideResetListener {
    fun onOverrideReset(experiment: Experiment)
}
