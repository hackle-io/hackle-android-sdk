package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.RelativeLayout
import androidx.core.view.WindowInsetsCompat
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageLifecycle
import io.hackle.android.ui.inappmessage.InAppMessageUi
import java.lang.ref.WeakReference

internal abstract class InAppMessageBaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr), InAppMessageView {

    private var _controller: InAppMessageViewController? = null
    private var _presentationContext: InAppMessagePresentationContext? = null
    private var _activity: WeakReference<Activity>? = null

    override val state: InAppMessageView.State get() = controller.state
    override val controller: InAppMessageViewController get() = requireNotNull(_controller) { "InAppMessageController is not set on InAppMessageBaseView." }
    override val presentationContext: InAppMessagePresentationContext get() = requireNotNull(_presentationContext) { "InAppMessagePresentationContext is not set on InAppMessageBaseView." }
    override val activity: Activity? get() = _activity?.get()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && InAppMessageUi.instance.isBackButtonDismisses) {
            close()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun setController(controller: InAppMessageViewController) {
        _controller = controller
    }

    fun setPresentationContext(context: InAppMessagePresentationContext) {
        _presentationContext = context
    }

    fun setActivity(activity: Activity) {
        _activity = WeakReference(activity)
    }

    override fun publish(lifecycle: InAppMessageLifecycle) {
        publishInAppMessageLifecycle(lifecycle)
    }

    abstract val openAnimator: InAppMessageAnimator?
    abstract val closeAnimator: InAppMessageAnimator?

    abstract fun configure()

    /**
     * Called to apply window insets to the view.
     * This function is a placeholder and does not perform any operation by default.
     * Subclasses can override this method to customize how window insets are handled.
     *
     * @param insets The window insets provided by the system, encapsulated in a
     * [WindowInsetsCompat] object. This contains information about areas such as
     * system bars that should not overlap with the content.
     */
    open fun onApplyWindowInsets(insets: WindowInsetsCompat) {
        // nothing to do
    }
}
