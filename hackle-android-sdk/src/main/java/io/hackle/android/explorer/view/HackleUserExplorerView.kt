package io.hackle.android.explorer.view

import android.app.Activity
import android.graphics.PixelFormat.TRANSLUCENT
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import io.hackle.android.R
import io.hackle.android.explorer.view.button.HackleUserExplorerButtonTouchHandler
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread

internal class HackleUserExplorerView {

    private var button: View? = null

    fun attachTo(activity: Activity) {
        if (button == null) {
            runOnUiThread {
                attach(activity)
            }
        }
    }

    private fun attach(activity: Activity) {
        val inflater = activity.layoutInflater
        val windowManager = activity.windowManager

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay?.getMetrics(displayMetrics)

        val params = layoutParams(displayMetrics)
        button = inflater.inflate(R.layout.hackle_view_user_explorer_button, null).apply {
            windowManager.addView(this, params)
            setOnTouchListener(HackleUserExplorerButtonTouchHandler(windowManager, params))
        }
    }

    private fun layoutParams(displayMetrics: DisplayMetrics): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            type = TYPE_APPLICATION
            format = TRANSLUCENT
            flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL
            y = displayMetrics.heightPixels / 2
            x = displayMetrics.widthPixels / 2
            height = WRAP_CONTENT
            width = WRAP_CONTENT
        }
    }
}
