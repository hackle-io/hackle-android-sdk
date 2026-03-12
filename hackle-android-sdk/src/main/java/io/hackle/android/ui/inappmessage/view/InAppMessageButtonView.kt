package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.Button
import io.hackle.android.ui.inappmessage.textColor
import io.hackle.sdk.core.model.InAppMessage

internal open class InAppMessageButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : Button(context, attrs, defStyleAttr) {

    fun configure(inAppMessageView: InAppMessageView, button: InAppMessage.Message.Button, background: Drawable) {
        this.text = button.text
        this.setTextColor(button.textColor)
        this.background = background
        this.setOnClickListener(inAppMessageView.createButtonClickListener(button))
    }
}
