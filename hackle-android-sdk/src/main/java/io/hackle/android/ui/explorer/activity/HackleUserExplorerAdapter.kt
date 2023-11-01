package io.hackle.android.ui.explorer.activity

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.hackle.android.ui.explorer.activity.experiment.ab.AbTestFragment
import io.hackle.android.ui.explorer.activity.experiment.ff.FeatureFlagFragment
import io.hackle.android.ui.explorer.activity.experiment.model.experimentType
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG

internal class HackleUserExplorerAdapter(
    fm: FragmentManager,
) : FragmentStatePagerAdapter(fm) {

    override fun getCount(): Int {
        return Experiment.Type.values().size
    }

    override fun getItem(position: Int): Fragment {
        return when (experimentType(position)) {
            AB_TEST -> AbTestFragment()
            FEATURE_FLAG -> FeatureFlagFragment()
        }
    }
}
