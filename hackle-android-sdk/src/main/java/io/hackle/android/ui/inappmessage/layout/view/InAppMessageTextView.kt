package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import io.hackle.android.ui.inappmessage.color
import io.hackle.sdk.core.model.InAppMessage

internal class InAppMessageTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    fun configure(attribute: InAppMessage.Message.Text.Attribute) {
        this.text = attribute.text
        this.setTextColor(attribute.color)
    }
}
