package io.hackle.android.explorer.activity.experiment.ab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.hackle.android.R
import io.hackle.android.explorer.base.HackleUserExplorer
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread

internal class AbTestFragment(
    private val explorer: HackleUserExplorer,
) : Fragment() {

    private lateinit var root: View
    private lateinit var itemView: ListView
    private lateinit var adapter: AbTestAdapter
    private lateinit var resetAll: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        root = inflater.inflate(R.layout.hackle_fragment_ab_test, container, false)
        itemView = root.findViewById(R.id.hackle_view_ab_test_items)
        adapter = AbTestAdapter(root.context, explorer)
        itemView.adapter = adapter

        resetAll = root.findViewById(R.id.hackle_ab_test_reset_all_button)
        resetAll.setOnClickListener {
            explorer.resetAllAbTestOverride()
            adapter.fetchAndUpdate()
            runOnUiThread {
                Toast.makeText(
                    context, getString(R.string.hackle_label_reset_all), Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }
}
