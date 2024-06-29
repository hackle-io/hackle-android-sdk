package io.hackle.android.ui.inappmessage.layout.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.core.model.InAppMessage

internal abstract class InAppMessageView : RelativeLayout, InAppMessageLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var _controller: InAppMessageController? = null
    private var _context: InAppMessagePresentationContext? = null
    private var _activity: Activity? = null

    override val controller: InAppMessageController get() = requireNotNull(_controller) { "InAppMessageController is not set on InAppMessageView." }
    override val context: InAppMessagePresentationContext get() = requireNotNull(_context) { "InAppMessagePresentationContext is not set on InAppMessageView." }
    override val activity: Activity? get() = _activity

    val inAppMessage: InAppMessage get() = context.inAppMessage
    val message: InAppMessage.Message get() = context.message

    fun setController(controller: InAppMessageViewController) {
        _controller = controller
    }

    fun setContext(context: InAppMessagePresentationContext) {
        _context = context
    }

    fun onOpen(activity: Activity) {
        _activity = activity
    }

    fun onClose() {
        _activity = null
        _controller = null
    }

    abstract fun layout()
}
