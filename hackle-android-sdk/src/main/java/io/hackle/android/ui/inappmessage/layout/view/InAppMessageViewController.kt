package io.hackle.android.ui.inappmessage.layout.view

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.core.setActivityRequestedOrientation
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.handle

internal class InAppMessageViewController(
    val view: InAppMessageView,
    override val context: InAppMessagePresentationContext,
    override val ui: InAppMessageUi,
) : InAppMessageController {

    override val layout: InAppMessageView get() = view
    private var originalOrientation: Int? = null

    override fun open(activity: Activity) {
        lockScreenOrientation(activity)

        val parent = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        parent.addView(view)
        ViewCompat.requestApplyInsets(parent)
        view.onOpen(activity)
        handle(InAppMessageEvent.Impression)
    }

    override fun close() {
        unlockScreenOrientation()

        val parent = view.parent as? ViewGroup ?: return
        handle(InAppMessageEvent.Close)
        parent.removeView(view)
        view.onClose()
        ui.closeCurrent()
    }

    private fun lockScreenOrientation(activity: Activity) {
        if (originalOrientation == null) {
            originalOrientation = activity.requestedOrientation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                activity.setActivityRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)
            }
        }
    }

    private fun unlockScreenOrientation() {
        val activity = view.activity
        val orientation = originalOrientation
        if (activity != null && orientation != null) {
            activity.setActivityRequestedOrientation(orientation)
            originalOrientation = null
        }
    }
}
