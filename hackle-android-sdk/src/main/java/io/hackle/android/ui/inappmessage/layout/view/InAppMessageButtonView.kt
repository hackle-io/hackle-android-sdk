package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.Button
import io.hackle.android.ui.inappmessage.backgroundColor
import io.hackle.android.ui.inappmessage.borderColor
import io.hackle.android.ui.inappmessage.px
import io.hackle.android.ui.inappmessage.textColor
import io.hackle.sdk.core.model.InAppMessage

internal class InAppMessageButtonView : Button {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(button: InAppMessage.Message.Button) {
        this.text = button.text
        this.setTextColor(button.textColor)

        val background = GradientDrawable()
        background.setColor(button.backgroundColor)
        background.setStroke(px(1).toInt(), button.borderColor)
        background.cornerRadius = px(4)
        this.background = background
    }

    fun bind(positionalButton: InAppMessage.Message.PositionalButton) {
        this.text = positionalButton.button.text
        this.setTextColor(positionalButton.button.textColor)
        this.background = ColorDrawable(Color.TRANSPARENT)
    }
}
