package io.hackle.android.ui.inappmessage.layout.view.sheet

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import io.hackle.android.R
import kotlin.math.min

internal class InAppMessageBottomSheetContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    // Attributes
    private val maxWidth get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_bottom_sheet_max_width)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val newWidth = min(measuredWidth, maxWidth)
        setMeasuredDimension(newWidth, measuredHeight)
    }
}
