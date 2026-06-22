package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.ui.core.setActivityRequestedOrientation
import io.hackle.android.ui.core.setFocusableInTouchModeAndRequestFocus
import io.hackle.android.ui.inappmessage.*
import io.hackle.android.ui.inappmessage.InAppMessageLifecycle.*
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.view.InAppMessageView.State
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.atomic.AtomicReference


internal class InAppMessageViewController(
    override val view: InAppMessageBaseView,
    override val ui: InAppMessageUi,
) : InAppMessageController {

    private val _state = AtomicReference(State.CREATED)
    val state: State get() = _state.get()

    private var originalOrientation: Int? = null

    override fun open(activity: Activity) {
        if (!_state.compareAndSet(State.CREATED, State.OPENING)) {
            log.debug { "InAppMessage cannot be opened (state=${state}, key=${view.inAppMessage.key})" }
            return
        }

        view.configure {
            runOnUiThread { present(activity) }
        }
    }

    private fun present(activity: Activity) {
        if (!_state.compareAndSet(State.OPENING, State.OPENED)) {
            log.debug { "InAppMessage is not opening (state=${state}, key=${view.inAppMessage.key})" }
            return
        }

        try {
            lifecycle(BEFORE_OPEN)
            addView(activity)
            startAnimation(view.openAnimator, completion = {
                handle(InAppMessageViewEvent.impression(view))
                lifecycle(AFTER_OPEN)
                view.setFocusableInTouchModeAndRequestFocus()
            })
        } catch (e: Throwable) {
            log.error { "Failed to present InAppMessage: $e" }
            close()
        }
    }

    override fun close(whenActivityDestroy: Boolean) {
        val prev: State = _state.getAndSet(State.CLOSED)
        return when (prev) {
            State.CREATED, State.OPENING -> {
                ui.closeCurrent()
            }

            State.OPENED -> {
                lifecycle(BEFORE_CLOSE)
                if (whenActivityDestroy) {
                    closeWithoutViewRemove()
                } else {
                    closeWithAnimation()
                }
            }

            State.CLOSED -> {
                log.debug { "InAppMessage is already closed (key=${view.inAppMessage.key})" }
            }
        }
    }

    private fun closeWithoutViewRemove() {
        handle(InAppMessageViewEvent.close(view))
        ui.closeCurrent()
        lifecycle(AFTER_CLOSE)
    }

    private fun closeWithAnimation() {
        startAnimation(view.closeAnimator, completion = {
            handle(InAppMessageViewEvent.close(view))
            removeView()
            lifecycle(AFTER_CLOSE)
        })
    }

    private fun lifecycle(lifecycle: InAppMessageLifecycle) {
        view.publish(lifecycle)
        ui.listener.onLifecycle(lifecycle, view.inAppMessage)
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

        val parent = view.parent as? ViewGroup
        parent?.removeView(view)

        ui.closeCurrent()
    }

    private fun lockScreenOrientation(activity: Activity) {
        if (originalOrientation == null) {
            originalOrientation = activity.requestedOrientation
            activity.setActivityRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)
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
