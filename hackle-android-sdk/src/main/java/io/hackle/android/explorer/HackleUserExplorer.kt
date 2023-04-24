package io.hackle.android.explorer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import io.hackle.android.R
import io.hackle.android.explorer.activity.HackleUserExplorerActivity
import io.hackle.android.explorer.base.HackleUserExplorerService
import io.hackle.android.explorer.view.button.HackleUserExplorerButton
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.sdk.core.internal.log.Logger

internal class HackleUserExplorer(
    val explorerService: HackleUserExplorerService,
) : Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null
    private var isShow: Boolean = false

    fun show() {
        isShow = true
        runOnUiThread {
            attach()
        }
    }

    private fun attach() {
        try {
            val activity = currentActivity ?: return
            if (activity is HackleUserExplorerActivity) {
                return
            }
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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (isShow) {
            attach()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    companion object {
        private val log = Logger<HackleUserExplorer>()
    }
}