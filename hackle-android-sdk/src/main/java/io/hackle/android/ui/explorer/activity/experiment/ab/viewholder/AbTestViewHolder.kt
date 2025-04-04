package io.hackle.android.ui.explorer.activity.experiment.ab.viewholder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import io.hackle.android.R
import io.hackle.android.ui.explorer.activity.experiment.listener.OnOverrideResetListener
import io.hackle.android.ui.explorer.activity.experiment.listener.OnOverrideSetListener
import io.hackle.android.ui.explorer.activity.experiment.model.isManualOverridable
import io.hackle.android.ui.explorer.base.ViewHolder
import io.hackle.android.ui.explorer.view.listener.OnSpinnerItemSelectedListener
import io.hackle.android.ui.explorer.view.listener.setOnSpinnerItemSelectedListener

internal class AbTestViewHolder private constructor(
    private val context: Context,
    private val overrideSetListener: OnOverrideSetListener,
    private val overrideResetListener: OnOverrideResetListener,
    override val itemView: View,
) : ViewHolder<AbTestItem> {

    private val key: TextView = itemView.findViewById(R.id.hackle_ab_test_key)
    private val desc: TextView = itemView.findViewById(R.id.hackle_ab_test_desc)
    private val spinner: Spinner = itemView.findViewById(R.id.hackle_ab_test_variation_spinner)
    private val resetButton: TextView = itemView.findViewById(R.id.hackle_ab_test_reset_button)

    override fun bind(item: AbTestItem) {
        key.text = item.keyLabel
        desc.text = item.descLabel

        val adapter = ArrayAdapter(context, R.layout.hackle_spinner_item, item.variationKeys)
        adapter.setDropDownViewResource(R.layout.hackle_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.setSelection(item.variationKeys.indexOf(item.decisionVariationKey))
        spinner.setOnSpinnerItemSelectedListener(VariationSelectedListener(item))
        spinner.isEnabled = item.decision.reason.isManualOverridable

        resetButton.isEnabled = item.isManualOverridden
        resetButton.setOnClickListener(ResetClickListener(item))
    }

    inner class VariationSelectedListener(private val item: AbTestItem) :
        OnSpinnerItemSelectedListener() {
        override fun onItemSelectedByUser(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long,
        ) {
            val variationKey = item.variationKeys[position]
            val variation = item.experiment.getVariationOrNull(variationKey) ?: return
            overrideSetListener.onOverrideSet(item.experiment, variation)
        }
    }

    inner class ResetClickListener(private val item: AbTestItem) : OnClickListener {
        override fun onClick(v: View?) {
            val variationKey = item.variationKeys[spinner.selectedItemPosition]
            val variation = item.experiment.getVariationOrNull(variationKey) ?: return
            overrideResetListener.onOverrideReset(item.experiment, variation)
        }
    }

    companion object {
        fun create(
            context: Context,
            overrideSetListener: OnOverrideSetListener,
            overrideResetListener: OnOverrideResetListener,
            parent: ViewGroup?,
        ): AbTestViewHolder {
            val inflater = LayoutInflater.from(context)
            val itemView = inflater.inflate(R.layout.hackle_view_ab_test_item, parent, false)
            return AbTestViewHolder(context, overrideSetListener, overrideResetListener, itemView)
        }
    }
}

private val AbTestItem.keyLabel get() = "[${experiment.key}] ${experiment.name ?: ""}"
private val AbTestItem.descLabel
    get() = listOf(
        "V${experiment.version}",
        experiment.status.name,
        experiment.variations.joinToString("/") { it.key },
        experiment.identifierType
    ).joinToString(" | ")
private val AbTestItem.variationKeys get() = experiment.variations.map { it.key }
private val AbTestItem.decisionVariationKey get() = decision.variation.name
private val AbTestItem.isManualOverridden get() = overriddenVariation != null
