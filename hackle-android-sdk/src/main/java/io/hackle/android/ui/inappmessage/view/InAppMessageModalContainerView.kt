package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import io.hackle.android.R
import io.hackle.sdk.core.model.InAppMessage
import kotlin.math.min

internal class InAppMessageModalContainerView : RelativeLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_min_width)
        val maxWidth = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_max_width)
        val maxHeight = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_max_height)

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val newMaxHeight = min(maxHeight, measuredHeight)
        if (measuredWidth < minWidth) {
            setMeasuredDimension(minWidth, measuredHeight)
        } else if (measuredWidth > maxWidth) {
            resize(maxWidth, newMaxHeight)
        } else {
            resize(measuredWidth, newMaxHeight)
        }
    }


    private fun resize(width: Int, height: Int) {
        val view = getChildAt(0)
        if (view is ImageView) {
            if (view.drawable != null) {
                val imageWidth = view.measuredWidth
                val newWidth = min(imageWidth, width)
                setMeasuredDimension(newWidth, height)
            } else {
                setMeasuredDimension(width, height)
            }
        }
    }

    fun setModalFrameStyle(message: InAppMessage.Message) {
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        val dpi = context.resources.displayMetrics.densityDpi
        gradientDrawable.cornerRadius = (8 * dpi / 160).toFloat() // 코너 반경 8dp
        gradientDrawable.setColor(Color.parseColor(message.background.color))
        background = gradientDrawable
    }

    fun setImageViewStyle(
        message: InAppMessage.Message,
        inAppMessageImageView: InAppMessageImageView
    ) {
        if (message.text != null) {
            inAppMessageImageView.setAspectRatio(TEXT_IMAGE_VIEW_ASPECT_RATIO)
        } else {
            inAppMessageImageView.setAspectRatio(NO_TEXT_IMAGE_VIEW_ASPECT_RATIO)
        }
    }

    companion object {
        private const val NO_TEXT_IMAGE_VIEW_ASPECT_RATIO = 200f / 300f
        private const val TEXT_IMAGE_VIEW_ASPECT_RATIO = 290f / 100f
    }
}