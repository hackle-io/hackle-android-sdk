package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.textColor
import io.hackle.sdk.core.model.InAppMessage

internal class InAppMessageCloseButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextView(context, attrs, defStyleAttr) {

    fun configure(inAppMessageView: InAppMessageView, closeButton: InAppMessage.Message.Button) {
        setTextColor(closeButton.textColor)
        setOnClickListener {
            val event = InAppMessageEvent.action(inAppMessageView, closeButton.action, InAppMessage.ActionArea.X_BUTTON)
            inAppMessageView.handle(event)
        }
    }
}
