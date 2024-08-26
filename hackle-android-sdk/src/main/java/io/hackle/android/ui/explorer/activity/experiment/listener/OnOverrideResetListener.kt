package io.hackle.android.ui.explorer.activity.experiment.listener

import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Variation

internal interface OnOverrideResetListener {
    fun onOverrideReset(experiment: Experiment, variation: Variation)
}
