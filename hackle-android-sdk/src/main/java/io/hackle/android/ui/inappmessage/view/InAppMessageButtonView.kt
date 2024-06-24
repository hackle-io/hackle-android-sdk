package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.Button
import io.hackle.android.ui.inappmessage.px
import io.hackle.sdk.core.model.InAppMessage

internal class InAppMessageButtonView : Button {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(button: InAppMessage.Message.Button) {
        text = button.text
        setTextColor(Color.parseColor(button.style.textColor))
        background = GradientDrawable().apply {
            setColor(Color.parseColor(button.style.bgColor))
            setStroke(1, Color.parseColor(button.style.borderColor))
            cornerRadius = px(4)
        }
    }

    fun bind(positionalButton: InAppMessage.Message.PositionalButton) {
        text = positionalButton.button.text
        setTextColor(Color.parseColor(positionalButton.button.style.textColor))
        background = ColorDrawable(Color.TRANSPARENT)
    }
}
