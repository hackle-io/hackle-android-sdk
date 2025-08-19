package io.hackle.android.ui.explorer.activity.experiment.ab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import io.hackle.android.Hackle
import io.hackle.android.R
import io.hackle.android.app
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread

internal class AbTestFragment : Fragment() {

    private lateinit var root: View
    private lateinit var itemView: ListView
    private lateinit var adapter: AbTestAdapter
    private lateinit var resetAll: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        val explorerService = Hackle.app.userExplorerService
        root = inflater.inflate(R.layout.hackle_fragment_ab_test, container, false)
        itemView = root.findViewById(R.id.hackle_view_ab_test_items)
        adapter = AbTestAdapter(root.context, explorerService)
        itemView.adapter = adapter

        resetAll = root.findViewById(R.id.hackle_ab_test_reset_all_button)
        resetAll.setOnClickListener {
            explorerService.resetAllAbTestOverride()
            adapter.fetchAndUpdate()
            runOnUiThread {
                Toast.makeText(context, getString(R.string.hackle_label_reset_all), LENGTH_SHORT)
                    .show()
            }
        }

        return root
    }
}
