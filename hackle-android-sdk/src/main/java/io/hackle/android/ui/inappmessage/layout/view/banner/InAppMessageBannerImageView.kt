package io.hackle.android.ui.inappmessage.layout.view.banner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.ImageView.ScaleType.FIT_CENTER
import io.hackle.android.R
import io.hackle.android.ui.inappmessage.image
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageCloseButtonView
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageImageView
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageView
import io.hackle.android.ui.inappmessage.layout.view.createMessageClickListener
import io.hackle.android.ui.inappmessage.requiredOrientation
import io.hackle.sdk.core.model.InAppMessage
import kotlin.math.min

internal class InAppMessageBannerImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : InAppMessageView(context, attrs, defStyleAttr) {

    // Attribute
    private val maxWidth get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_max_width)
    private val horizontalMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_horizontal_margin)
    private val topMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_top_margin)
    private val bottomMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_bottom_margin)
    private val cornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_corner_radius)

    // View
    private val imageView: InAppMessageImageView get() = findViewById(R.id.hackle_iam_bannerimage_image_view)
    private val closeButtonView: InAppMessageCloseButtonView get() = findViewById(R.id.hackle_iam_bannerimage_close_button)

    // Model
    private val messageAlignment: InAppMessage.Message.Alignment get() = requireNotNull(message.layout.alignment) { "Not found Alignment in banner in-app message [${inAppMessage.id}]" }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val newWidth = min(maxWidth, measuredWidth)
        setMeasuredDimension(newWidth, measuredHeight)
    }

    override fun configure() {

        // Frame
        this.layoutParams = frameLayoutParams()

        // Image
        val image = context.image(requiredOrientation)
        imageView.configure(this, image, FIT_CENTER)
        imageView.setAspectRatio(IMAGE_VIEW_ASPECT_RATIO)
        imageView.setCornersRadius(cornerRadius.toFloat())
        imageView.setOnClickListener(createMessageClickListener())

        // CloseButton
        val closeButton = message.closeButton
        if (closeButton != null) {
            closeButtonView.configure(this, closeButton)
        } else {
            closeButtonView.visibility = View.GONE
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

        private val IMAGE_VIEW_ASPECT_RATIO = InAppMessageImageView.AspectRatio(width = 288f, height = 84f)

        fun create(activity: Activity): InAppMessageBannerImageView {
            @SuppressLint("InflateParams")
            val view = activity.layoutInflater.inflate(R.layout.hackle_iam_bannerimage, null)
            return view as InAppMessageBannerImageView
        }
    }
}