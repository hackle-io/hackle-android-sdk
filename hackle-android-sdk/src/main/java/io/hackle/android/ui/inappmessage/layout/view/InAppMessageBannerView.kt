package io.hackle.android.ui.inappmessage.layout.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import io.hackle.android.R
import io.hackle.android.ui.inappmessage.*
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.layout.handle
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionArea.MESSAGE
import io.hackle.sdk.core.model.InAppMessage.ActionArea.X_BUTTON
import kotlin.math.min

internal class InAppMessageBannerView : InAppMessageView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    // Attribute
    private val maxWidth get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_max_width)
    private val maxHeight get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_max_height)
    private val horizontalMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_horizontal_margin)
    private val topMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_top_margin)
    private val bottomMargin get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_bottom_margin)
    private val cornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_corner_radius)
    private val imageCornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_banner_image_corner_radius)

    // View
    private val imageView: InAppMessageImageView get() = findViewById(R.id.hackle_iam_banner_image_view)
    private val textView: TextView get() = findViewById(R.id.hackle_iam_banner_text_view)
    private val closeButtonView: TextView get() = findViewById(R.id.hackle_iam_banner_close_button)

    // Model
    private val messageAlignment: InAppMessage.Message.Alignment get() = requireNotNull(message.layout.alignment) { "Not found Alignment in banner in-app message [${inAppMessage.id}]" }
    private val messageText: InAppMessage.Message.Text get() = requireNotNull(message.text) { "Not found Text in banner in-app message [${inAppMessage.id}]" }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(min(maxWidth, measuredWidth), maxHeight)
    }

    override fun layout() {
        val orientation = orientation
        if (orientation == null || !context.inAppMessage.supports(orientation)) {
            close()
            return
        }

        // Frame
        this.layoutParams = frameLayoutParams()

        val background = GradientDrawable()
        background.cornerRadius = cornerRadius.toFloat()
        background.setColor(message.backgroundColor)
        this.background = background
        this.setOnClickListener {
            val action = message.action
            if (action != null) {
                handle(InAppMessageEvent.Action(action, MESSAGE))
            }
        }

        // ImageView
        val image = context.imageOrNull(orientation)
        if (image != null) {
            imageView.render(image, this)
            imageView.setCornersRadius(imageCornerRadius.toFloat())
        } else {
            imageView.visibility = View.GONE
        }

        // TextView
        textView.text = messageText.body.text
        textView.setTextColor(messageText.body.color)

        // CloseButton
        val closeButton = message.closeButton
        if (closeButton != null) {
            closeButtonView.visibility = View.VISIBLE
            closeButtonView.setTextColor(closeButton.textColor)
            closeButtonView.setOnClickListener {
                handle(InAppMessageEvent.Action(closeButton.action, X_BUTTON))
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
        fun create(activity: Activity): InAppMessageBannerView {
            @SuppressLint("InflateParams")
            val inflate = activity.layoutInflater.inflate(R.layout.hackle_iam_banner, null)
            return inflate as InAppMessageBannerView
        }
    }
}

internal val InAppMessage.Message.Alignment.bannerGravity: Int
    get() = when (vertical) {
        InAppMessage.Message.Alignment.Vertical.BOTTOM -> (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        InAppMessage.Message.Alignment.Vertical.MIDDLE -> Gravity.CENTER
        InAppMessage.Message.Alignment.Vertical.TOP -> (Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    }
