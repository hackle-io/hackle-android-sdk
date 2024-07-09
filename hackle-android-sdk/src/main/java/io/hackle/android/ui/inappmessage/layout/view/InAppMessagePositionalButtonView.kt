package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import io.hackle.android.R
import io.hackle.sdk.core.model.InAppMessage

internal class InAppMessagePositionalButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : InAppMessageButtonView(context, attrs, defStyleAttr) {

    val alignment: InAppMessage.Message.Alignment

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.InAppMessagePositionalButtonView)
        alignment = alignment(attributes)
        attributes.recycle()
    }

    private fun alignment(attributes: TypedArray): InAppMessage.Message.Alignment {
        val horizontal = when (attributes.getInt(R.styleable.InAppMessagePositionalButtonView_alignHorizontal, -1)) {
            0 -> InAppMessage.Message.Alignment.Horizontal.LEFT
            1 -> InAppMessage.Message.Alignment.Horizontal.CENTER
            2 -> InAppMessage.Message.Alignment.Horizontal.RIGHT
            else -> throw IllegalArgumentException("Invalid in-app message alignment attribute")
        }
        val vertical = when (attributes.getInt(R.styleable.InAppMessagePositionalButtonView_alignVertical, -1)) {
            0 -> InAppMessage.Message.Alignment.Vertical.TOP
            1 -> InAppMessage.Message.Alignment.Vertical.MIDDLE
            2 -> InAppMessage.Message.Alignment.Vertical.BOTTOM
            else -> throw IllegalArgumentException("Invalid in-app message alignment attribute")
        }
        return InAppMessage.Message.Alignment(horizontal, vertical)
    }
}
