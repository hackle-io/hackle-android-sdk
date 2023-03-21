package io.hackle.android.explorer.activity.experiment.listener

import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Variation

internal interface OnOverrideSetListener {
    fun onOverrideSet(experiment: Experiment, variation: Variation)
}
