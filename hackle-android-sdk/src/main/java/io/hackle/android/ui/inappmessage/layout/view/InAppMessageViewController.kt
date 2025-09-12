package io.hackle.android.ui.inappmessage.layout.view

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.core.setActivityRequestedOrientation
import io.hackle.android.ui.core.setFocusableInTouchModeAndRequestFocus
import io.hackle.android.ui.inappmessage.*
import io.hackle.android.ui.inappmessage.InAppMessageLifecycle.*
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.layout.InAppMessageAnimator
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout.State
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.atomic.AtomicReference


internal class InAppMessageViewController(
    val view: InAppMessageView,
    override val context: InAppMessagePresentationContext,
    override val ui: InAppMessageUi,
) : InAppMessageController {

    override val layout: InAppMessageView get() = view
    private var originalOrientation: Int? = null

    private val _state = AtomicReference(State.CLOSED)
    val state: State get() = _state.get()

    override fun open(activity: Activity) {
        if (!_state.compareAndSet(State.CLOSED, State.OPENED)) {
            log.debug { "InAppMessage is already open (key=${context.inAppMessage.key})" }
            return
        }

        lifecycle(BEFORE_OPEN)
        addView(activity)
        startAnimation(view.openAnimator, completion = {
            handle(InAppMessageEvent.Impression)
            lifecycle(AFTER_OPEN)
            view.setFocusableInTouchModeAndRequestFocus()
        })
    }

    override fun close() {
        if (!_state.compareAndSet(State.OPENED, State.CLOSED)) {
            log.debug { "InAppMessage is already close (key=${context.inAppMessage.key})" }
            return
        }

        lifecycle(BEFORE_CLOSE)
        startAnimation(view.closeAnimator, completion = {
            handle(InAppMessageEvent.Close)
            removeView()
            lifecycle(AFTER_CLOSE)
        })
    }

    private fun lifecycle(lifecycle: InAppMessageLifecycle) {
        view.publish(lifecycle)
        ui.listener.onLifecycle(lifecycle, context.inAppMessage)
    }

    private fun addView(activity: Activity) {
        lockScreenOrientation(activity)

        val parent = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        parent.addView(view)
        ViewCompat.requestApplyInsets(parent)
        view.setActivity(activity)
    }

    private fun removeView() {
        unlockScreenOrientation()

        val parent = view.parent as? ViewGroup ?: return
        parent.removeView(view)
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

    private fun startAnimation(animator: InAppMessageAnimator?, completion: () -> Unit) {
        if (animator != null) {
            animator.setListener(object : InAppMessageAnimator.Listener {
                override fun onAnimationEnd() {
                    completion()
                }
            })
            animator.start()
        } else {
            completion()
        }
    }

    companion object {
        private val log = Logger<InAppMessageViewController>()
    }
}
