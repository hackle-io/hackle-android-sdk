package io.hackle.android.ui.inappmessage.layout.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.widget.RelativeLayout
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.layout.InAppMessageAnimator
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.android.ui.inappmessage.layout.handle
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionArea.IMAGE
import io.hackle.sdk.core.model.InAppMessage.ActionArea.MESSAGE
import java.lang.ref.WeakReference

internal abstract class InAppMessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), InAppMessageLayout {

    private var _controller: InAppMessageViewController? = null
    private var _context: InAppMessagePresentationContext? = null
    private var _activity: WeakReference<Activity>? = null

    override val state: InAppMessageLayout.State get() = controller.state
    override val controller: InAppMessageViewController get() = requireNotNull(_controller) { "InAppMessageController is not set on InAppMessageView." }
    override val context: InAppMessagePresentationContext get() = requireNotNull(_context) { "InAppMessagePresentationContext is not set on InAppMessageView." }
    override val activity: Activity? get() = _activity?.get()

    val inAppMessage: InAppMessage get() = context.inAppMessage
    val message: InAppMessage.Message get() = context.message

    fun setController(controller: InAppMessageViewController) {
        _controller = controller
    }

    fun setContext(context: InAppMessagePresentationContext) {
        _context = context
    }

    fun setActivity(activity: Activity) {
        _activity = WeakReference(activity)
    }

    abstract val openAnimator: InAppMessageAnimator?
    abstract val closeAnimator: InAppMessageAnimator?

    abstract fun configure()
}

internal fun InAppMessageView.createCloseListener(): OnClickListener {
    return OnClickListener { close() }
}

internal fun InAppMessageView.createMessageClickListener(): OnClickListener {
    return OnClickListener {
        val action = message.action ?: return@OnClickListener
        handle(InAppMessageEvent.Action(action, MESSAGE))
    }
}

internal fun InAppMessageView.createImageClickListener(image: InAppMessage.Message.Image): OnClickListener {
    return OnClickListener {
        val action = image.action ?: return@OnClickListener
        handle(InAppMessageEvent.Action(action, IMAGE))
    }
}
