package io.hackle.android.ui.explorer.activity.experiment.ff.viewholder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import io.hackle.android.R
import io.hackle.android.ui.explorer.activity.experiment.listener.OnOverrideResetListener
import io.hackle.android.ui.explorer.activity.experiment.listener.OnOverrideSetListener
import io.hackle.android.ui.explorer.activity.experiment.model.isManualOverridable
import io.hackle.android.ui.explorer.base.ViewHolder
import io.hackle.android.ui.explorer.view.listener.OnSpinnerItemSelectedListener
import io.hackle.android.ui.explorer.view.listener.setOnSpinnerItemSelectedListener

internal class FeatureFlagViewHolder private constructor(
    private val context: Context,
    private val spinnerListener: OnOverrideSetListener,
    private val resetListener: OnOverrideResetListener,
    override val itemView: View,
) : ViewHolder<FeatureFlagItem> {

    private val key: TextView = itemView.findViewById(R.id.hackle_feature_flag_key)
    private val desc: TextView = itemView.findViewById(R.id.hackle_feature_flag_desc)
    private val spinner: Spinner = itemView.findViewById(R.id.hackle_feature_flag_variation_spinner)
    private val resetButton: Button = itemView.findViewById(R.id.hackle_feature_flag_reset_button)

    override fun bind(item: FeatureFlagItem) {
        key.text = item.keyLabel
        desc.text = item.descLabel

        val spinnerItems = item.variationKeys.map { it != "A" }
        val adapter = ArrayAdapter(context, R.layout.hackle_spinner_item, spinnerItems)
        adapter.setDropDownViewResource(R.layout.hackle_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(spinnerItems.indexOf(item.decision.isOn))
        spinner.setOnSpinnerItemSelectedListener(VariationSelectedListener(item))
        spinner.isEnabled = item.decision.reason.isManualOverridable

        resetButton.isEnabled = item.isManualOverridden
        resetButton.setOnClickListener {
            resetListener.onOverrideReset(item.experiment)
        }
    }

    inner class VariationSelectedListener(private val item: FeatureFlagItem) :
        OnSpinnerItemSelectedListener() {
        override fun onItemSelectedByUser(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long,
        ) {
            val variationKey = item.variationKeys[position]
            val variation = item.experiment.getVariationOrNull(variationKey) ?: return
            spinnerListener.onOverrideSet(item.experiment, variation)
        }
    }

    companion object {
        fun create(
            context: Context,
            spinnerListener: OnOverrideSetListener,
            resetListener: OnOverrideResetListener,
            parent: ViewGroup?,
        ): FeatureFlagViewHolder {
            val inflater = LayoutInflater.from(context)
            val itemView = inflater.inflate(R.layout.hackle_view_feature_flag_item, parent, false)
            return FeatureFlagViewHolder(context, spinnerListener, resetListener, itemView)
        }
    }
}

private val FeatureFlagItem.keyLabel get() = "[${experiment.key}] ${experiment.name ?: ""}"
private val FeatureFlagItem.descLabel get() = experiment.status.name
private val FeatureFlagItem.variationKeys get() = experiment.variations.map { it.key }
private val FeatureFlagItem.isManualOverridden get() = overriddenVariation != null