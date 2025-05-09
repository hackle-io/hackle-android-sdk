package io.hackle.android.ui.explorer

import android.app.Activity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import io.hackle.android.R
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.Lifecycle
import io.hackle.android.internal.lifecycle.LifecycleListener
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.ui.explorer.base.HackleUserExplorerService
import io.hackle.android.ui.explorer.view.button.HackleUserExplorerButton
import io.hackle.sdk.core.internal.log.Logger

internal class HackleUserExplorer(
    val explorerService: HackleUserExplorerService,
    private val activityProvider: ActivityProvider,
) : LifecycleListener {

    private var isShow: Boolean = false

    fun show() {
        isShow = true
        val activity = activityProvider.currentActivity ?: return
        runOnUiThread {
            attach(activity)
        }
    }

    fun hide() {
        isShow = false
        val activity = activityProvider.currentActivity ?: return
        runOnUiThread {
            detach(activity)
        }

    }

    private fun attach(activity: Activity) {
        try {
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

    private fun detach(activity: Activity) {
        activity.findViewById<FrameLayout>(R.id.hackle_user_explorer_view)?.let { view ->
            try {
                (view.parent as? ViewGroup)?.removeView(view)
            } catch (e: Throwable) {
                log.error { "Failed to detach HackleUserExplorer: $e" }
            }
        }
    }

    override fun onLifecycle(lifecycle: Lifecycle, activity: Activity, timestamp: Long) {
        if (lifecycle == Lifecycle.RESUMED) {
            if (isShow) {
                attach(activity)
            }
        }
    }

    companion object {
        private val log = Logger<HackleUserExplorer>()
    }
}
