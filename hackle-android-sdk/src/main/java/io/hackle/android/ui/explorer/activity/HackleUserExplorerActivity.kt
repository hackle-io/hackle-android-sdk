package io.hackle.android.ui.explorer.activity

import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import io.hackle.android.Hackle
import io.hackle.android.R
import io.hackle.android.app
import io.hackle.android.ui.HackleActivity
import io.hackle.android.ui.explorer.activity.experiment.view.ExperimentTabLayout
import io.hackle.android.ui.explorer.activity.user.IdentifierItem
import io.hackle.android.ui.explorer.activity.user.IdentifierView

internal class HackleUserExplorerActivity : FragmentActivity(), HackleActivity {

    private lateinit var closeButton: ImageView

    private lateinit var defaultId: IdentifierView
    private lateinit var deviceId: IdentifierView
    private lateinit var userId: IdentifierView

    private lateinit var tab: ExperimentTabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var adapter: HackleUserExplorerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hackle_activity_user_explorer)

        val explorerService = Hackle.app.userExplorer.explorerService
        val user = explorerService.currentUser()

        closeButton = findViewById(R.id.hackle_user_explorer_close_button)
        closeButton.setOnClickListener { onBackPressed() }

        defaultId = findViewById(R.id.hackle_identifier_default_id)
        defaultId.bind(IdentifierItem(getString(R.string.hackle_label_id), user.id))
        deviceId = findViewById(R.id.hackle_identifier_device_id)
        deviceId.bind(IdentifierItem(getString(R.string.hackle_label_device_id), user.deviceId))
        userId = findViewById(R.id.hackle_identifier_user_id)
        userId.bind(IdentifierItem(getString(R.string.hackle_label_user_id), user.userId))

        viewPager = findViewById(R.id.hackle_user_explorer_view_pager)
        adapter = HackleUserExplorerAdapter(explorerService, supportFragmentManager)
        viewPager.adapter = adapter
        tab = findViewById(R.id.hackle_user_explorer_tab)
        viewPager.addOnPageChangeListener(ExperimentTabLayout.ExperimentOnPageChangeListener(tab))
        tab.addOnTabSelectedListener(ExperimentTabLayout.ViewPagerOnTabSelectedListener(viewPager))
    }
}
