package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import io.hackle.android.R
import io.hackle.android.ui.inappmessage.images
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageImageView.AspectRatio
import io.hackle.sdk.core.model.InAppMessage
import kotlin.math.min

internal class InAppMessageImageContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val imageView: InAppMessageImageView
    private val scrollImageView: InAppMessageScrollImageView

    private var aspectRatio: AspectRatio? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.hackle_iam_image_container, this, true)
        imageView = view.findViewById(R.id.hackle_iam_image_container_image_view)
        scrollImageView = view.findViewById(R.id.hackle_iam_image_container_scroll_image_view)
    }

    fun setAspectRatio(aspectRatio: AspectRatio?) {
        this.aspectRatio = aspectRatio
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val aspectRatio = aspectRatio
        if (aspectRatio != null && measuredWidth > 0) {
            val newWidth = measuredWidth
            val maxHeight = aspectRatio.calculateHeight(measuredWidth)
            val newHeight = if (measuredHeight > 0) min(measuredHeight, maxHeight) else maxHeight
            setMeasuredDimension(newWidth, newHeight + 1)
        } else {
            setMeasuredDimension(measuredWidth, measuredHeight)
        }
    }

    fun configure(inAppMessageView: InAppMessageView, orientation: InAppMessage.Orientation) {
        val images = inAppMessageView.context.images(orientation)
        when (images.size) {
            0 -> {
                visibility = GONE
            }

            1 -> {
                val image = images.first()
                imageView.setAspectRatio(aspectRatio)
                imageView.configure(inAppMessageView, image, ImageView.ScaleType.FIT_XY)
                imageView.setOnClickListener(inAppMessageView.createImageClickListener(image, null))
                scrollImageView.visibility = GONE
            }

            else -> {
                scrollImageView.configure(inAppMessageView, images)
                imageView.visibility = GONE
            }
        }
    }
}
