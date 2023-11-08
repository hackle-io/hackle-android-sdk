package io.hackle.android.ui.explorer

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import io.hackle.android.R
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.LifecycleManager.LifecycleState
import io.hackle.android.internal.lifecycle.LifecycleManager.LifecycleStateListener
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.ui.explorer.base.HackleUserExplorerService
import io.hackle.android.ui.explorer.view.button.HackleUserExplorerButton
import io.hackle.sdk.core.internal.log.Logger

internal class HackleUserExplorer(
    val explorerService: HackleUserExplorerService,
    private val activityProvider: ActivityProvider,
) : LifecycleStateListener {

    private var isShow: Boolean = false

    fun show() {
        isShow = true
        runOnUiThread {
            attach()
        }
    }

    private fun attach() {
        try {
            val activity = activityProvider.currentActivity ?: return

            if (activity.findViewById<FrameLayout>(R.id.hackle_user_explorer_view) != null) {
                return
            }

            val view = FrameLayout(activity)
            view.id = R.id.hackle_user_explorer_view
            view.clipChildren = false
            view.clipToPadding = false
            view.fitsSystemWindows = true
            view.addView(HackleUserExplorerButton(activity))
            activity.addContentView(view, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        } catch (e: Throwable) {
            log.error { "Failed to attach HackleUserExplorer: $e" }
        }
    }

    override fun onState(state: LifecycleState, timestamp: Long) {
        when (state) {
            LifecycleState.FOREGROUND -> {
                activityProvider.currentActivity ?: return
                if (isShow) {
                    attach()
                }
            }
            LifecycleState.BACKGROUND -> {}
        }
    }

    companion object {
        private val log = Logger<HackleUserExplorer>()
    }
}
