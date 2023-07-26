package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import kotlin.math.min

internal class InAppMessageBodyTextView : TextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        val maxHeight = min(measuredHeight, (parentHeight * 0.4).toInt())
        setMeasuredDimension(measuredWidth, maxHeight)
    }
}