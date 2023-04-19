package io.hackle.android.explorer.activity

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.hackle.android.explorer.activity.experiment.ab.AbTestFragment
import io.hackle.android.explorer.activity.experiment.ff.FeatureFlagFragment
import io.hackle.android.explorer.activity.experiment.model.experimentType
import io.hackle.android.explorer.base.HackleUserExplorerService
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG

internal class HackleUserExplorerAdapter(
    private val explorerService: HackleUserExplorerService,
    fm: FragmentManager,
) : FragmentStatePagerAdapter(fm) {

    override fun getCount(): Int {
        return Experiment.Type.values().size
    }

    override fun getItem(position: Int): Fragment {
        return when (experimentType(position)) {
            AB_TEST -> AbTestFragment(explorerService)
            FEATURE_FLAG -> FeatureFlagFragment(explorerService)
        }
    }
}
