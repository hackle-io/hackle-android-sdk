package io.hackle.android.ui.inappmessage.layout.view.modal

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.hackle.android.R

internal class InAppMessageModalContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // Attributes
    private val minWidth get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_min_width)
    private val maxWidth get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_max_width)

    private var maxWidthRatio: Double = 1.0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val maxWidth = (maxWidth * maxWidthRatio).toInt()
        val newWidth = when {
            measuredWidth < minWidth -> minWidth
            measuredWidth > maxWidth -> maxWidth
            else -> measuredWidth
        }
        setMeasuredDimension(newWidth, measuredHeight)
    }

    fun setMaxWidthRatio(ratio: Double) {
        this.maxWidthRatio = ratio
    }
}
