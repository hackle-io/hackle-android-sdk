package io.hackle.android.explorer.activity.experiment.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import io.hackle.android.R
import io.hackle.android.explorer.activity.experiment.model.experimentType
import io.hackle.android.explorer.activity.experiment.model.position
import io.hackle.sdk.core.model.Experiment.Type.AB_TEST
import io.hackle.sdk.core.model.Experiment.Type.FEATURE_FLAG

internal class ExperimentTabLayout : FrameLayout {

    private val abTest: TextView
    private val featureFlag: TextView

    private val listeners = mutableListOf<OnTabSelectedListener>()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.hackle_view_experiment_tab, this, true)
        abTest = view.findViewById(R.id.hackle_experiment_ab_test_tab)
        featureFlag = view.findViewById(R.id.hackle_experiment_feature_flag_tab)
        select(abTest)
        unselect(featureFlag)

        abTest.setOnClickListener { select(AB_TEST.position) }
        featureFlag.setOnClickListener { select(FEATURE_FLAG.position) }
    }

    fun select(position: Int) {
        return when (experimentType(position)) {
            AB_TEST -> {
                select(abTest)
                unselect(featureFlag)
            }
            FEATURE_FLAG -> {
                select(featureFlag)
                unselect(abTest)
            }
        }.also {
            listeners.forEach { it.onTabSelected(position) }
        }
    }

    private fun select(view: TextView) {
        view.setTextColor(resources.getColor(R.color.hackle_black))
        view.background = resources.getDrawable(R.drawable.hackle_tab_selected)
    }

    private fun unselect(view: TextView) {
        view.setTextColor(resources.getColor(R.color.hackle_dark_gray))
        view.background = resources.getDrawable(R.color.hackle_white)
    }

    fun addOnTabSelectedListener(listener: OnTabSelectedListener) {
        listeners.add(listener)
    }

    fun interface OnTabSelectedListener {
        fun onTabSelected(position: Int)
    }

    class ViewPagerOnTabSelectedListener(private val viewPager: ViewPager) : OnTabSelectedListener {
        override fun onTabSelected(position: Int) {
            viewPager.currentItem = position
        }
    }

    class ExperimentOnPageChangeListener(private val tabLayout: ExperimentTabLayout) :
        ViewPager.OnPageChangeListener {

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int,
        ) {
        }

        override fun onPageSelected(position: Int) {
            tabLayout.select(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
        }
    }
}
