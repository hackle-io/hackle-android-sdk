package io.hackle.android.explorer

import android.app.Activity
import android.graphics.PixelFormat.TRANSLUCENT
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import io.hackle.android.R

class HackleUserExplorer {

    private var bubbleView: View? = null

    fun show(activity: Activity) {
        if (bubbleView == null) {
            view(activity)
        }
    }

    private fun view(activity: Activity) {
        Handler(Looper.getMainLooper()).post {
            val windowManager = activity.windowManager
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay?.getMetrics(displayMetrics)
            val layoutParams = prepare(displayMetrics)
            bubbleView =
                activity.layoutInflater.inflate(R.layout.hackle_user_explorer_bubble_view, null)
            windowManager.addView(bubbleView, layoutParams)
            bubbleView?.setOnTouchListener(
                HackleUserExplorerTouchHandler(windowManager, layoutParams))
        }
    }

    private fun prepare(displayMetrics: DisplayMetrics): WindowManager.LayoutParams {
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
