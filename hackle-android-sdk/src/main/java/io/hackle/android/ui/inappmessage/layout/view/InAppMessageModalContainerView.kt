package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.RelativeLayout
import io.hackle.android.R
import io.hackle.android.ui.inappmessage.backgroundColor
import io.hackle.sdk.core.model.InAppMessage
import kotlin.math.min

internal open class InAppMessageModalContainerView : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    // Attribute
    private val minWidth get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_min_width)
    private val maxWidth get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_max_width)
    private val maxHeight get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_max_height)
    private val cornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_corner_radius)
    protected open val imageLayoutAspectRatio get() = 200f / 300f
    protected open val imageTextLayoutAspectRatio get() = 290f / 100f

    // View
    private val imageView: InAppMessageImageView get() = findViewById(R.id.hackle_iam_modal_image_view)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val newHeight = min(maxHeight, measuredHeight)
        if (measuredWidth < minWidth) {
            setMeasuredDimension(minWidth, newHeight)
        } else if (measuredWidth > maxWidth) {
            resize(maxWidth, newHeight)
        } else {
            resize(measuredWidth, newHeight)
        }
    }

    private fun resize(width: Int, height: Int) {
        if (imageView.drawable != null) {
            val imageWidth = imageView.measuredWidth
            val newWidth = min(imageWidth, width)
            setMeasuredDimension(newWidth, height)
        } else {
            setMeasuredDimension(width, height)
        }
    }

    fun setModalFrameStyle(message: InAppMessage.Message) {
        if (message.buttons.isEmpty() && message.text == null) {
            return
        }
        val background = GradientDrawable()
        background.shape = GradientDrawable.RECTANGLE
        background.cornerRadius = cornerRadius.toFloat()
        background.setColor(message.backgroundColor)
        this.background = background
    }

    fun setImageViewStyle(message: InAppMessage.Message, inAppMessageImageView: InAppMessageImageView) {
        if (message.text != null) {
            inAppMessageImageView.setAspectRatio(imageTextLayoutAspectRatio)
        } else {
            inAppMessageImageView.setAspectRatio(imageLayoutAspectRatio)
        }
    }
}

internal class InAppMessageModalLandContainerView : InAppMessageModalContainerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override val imageLayoutAspectRatio get() = 300f / 200f
    override val imageTextLayoutAspectRatio get() = 290f / 100f
}
