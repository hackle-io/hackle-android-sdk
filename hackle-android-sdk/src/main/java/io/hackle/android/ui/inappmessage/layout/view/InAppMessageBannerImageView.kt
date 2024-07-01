package io.hackle.android.ui.inappmessage.layout.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import io.hackle.android.R
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.image
import io.hackle.android.ui.inappmessage.layout.handle
import io.hackle.android.ui.inappmessage.orientation
import io.hackle.android.ui.inappmessage.supports
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionArea.MESSAGE
import kotlin.math.min

internal class InAppMessageBannerImageView : InAppMessageView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    // Attribute
    private val maxWidth get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_max_width)
    private val horizontalMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_horizontal_margin)
    private val topMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_top_margin)
    private val bottomMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_bottom_margin)
    private val cornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_corner_radius)

    // View
    private val imageView: InAppMessageImageView get() = findViewById(R.id.hackle_iam_bannerimage_image_view)
    private val closeButtonView: TextView get() = findViewById(R.id.hackle_iam_bannerimage_close_button)

    // Model
    private val messageAlignment: InAppMessage.Message.Alignment get() = requireNotNull(message.layout.alignment) { "Not found Alignment in banner in-app message [${inAppMessage.id}]" }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val newWidth = min(maxWidth, measuredWidth)
        setMeasuredDimension(newWidth, measuredHeight)
    }

    override fun layout() {
        val orientation = orientation
        if (orientation == null || !context.inAppMessage.supports(orientation)) {
            close()
            return
        }

        // Frame
        this.layoutParams = frameLayoutParams()

        // Image
        val image = context.image(orientation)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.render(image, this)
        imageView.setAspectRatio(IMAGE_VIEW_ASPECT_RATIO)
        imageView.setCornersRadius(cornerRadius.toFloat())
        imageView.setOnClickListener {
            val action = message.action
            if (action != null) {
                handle(InAppMessageEvent.Action(action, MESSAGE))
            }
        }

        // CloseButton
        message.closeButton?.let { closeButton ->
            closeButtonView.visibility = View.VISIBLE
            closeButtonView.setTextColor(Color.parseColor(closeButton.style.textColor))
            closeButtonView.setOnClickListener {
                handle(InAppMessageEvent.Action(closeButton.action, InAppMessage.ActionArea.X_BUTTON))
            }
        }
    }

    private fun frameLayoutParams(): FrameLayout.LayoutParams {
        val layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.width = maxWidth
        layoutParams.gravity = messageAlignment.bannerGravity
        layoutParams.setMargins(horizontalMargin, topMargin, horizontalMargin, bottomMargin)
        return layoutParams
    }

    companion object {

        private const val IMAGE_VIEW_ASPECT_RATIO = 288f / 84f

        fun create(activity: Activity): InAppMessageBannerImageView {
            @SuppressLint("InflateParams")
            val inflate = activity.layoutInflater.inflate(R.layout.hackle_iam_bannerimage, null)
            return inflate as InAppMessageBannerImageView
        }
    }
}
