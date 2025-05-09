package io.hackle.android.ui.inappmessage.layout.view.banner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.ImageView.ScaleType.FIT_CENTER
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import io.hackle.android.R
import io.hackle.android.ui.core.Animations
import io.hackle.android.ui.core.CornerRadii
import io.hackle.android.ui.core.Drawables
import io.hackle.android.ui.inappmessage.backgroundColor
import io.hackle.android.ui.inappmessage.imageOrNull
import io.hackle.android.ui.inappmessage.layout.InAppMessageAnimator
import io.hackle.android.ui.inappmessage.layout.view.*
import io.hackle.android.ui.inappmessage.requiredOrientation
import io.hackle.sdk.core.model.InAppMessage
import kotlin.math.min

internal class InAppMessageBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : InAppMessageView(context, attrs, defStyleAttr) {

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
    private val textView: InAppMessageTextView get() = findViewById(R.id.hackle_iam_banner_text_view)
    private val closeButtonView: InAppMessageCloseButtonView get() = findViewById(R.id.hackle_iam_banner_close_button)

    // Model
    private val messageAlignment: InAppMessage.Message.Alignment get() = requireNotNull(message.layout.alignment) { "Not found Alignment in banner in-app message [${inAppMessage.id}]" }
    private val messageText: InAppMessage.Message.Text get() = requireNotNull(message.text) { "Not found Text in banner in-app message [${inAppMessage.id}]" }

    // Animation
    override val openAnimator: InAppMessageAnimator get() = InAppMessageAnimator.of(this, Animations.fadeIn(50))
    override val closeAnimator: InAppMessageAnimator get() = InAppMessageAnimator.of(this, Animations.fadeOut(50))

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(min(maxWidth, measuredWidth), maxHeight)
    }

    override fun configure() {

        // ContainerView (inside of banner)
        this.layoutParams = messageLayoutParams
        this.background = messageBackground
        this.setOnClickListener(createMessageClickListener())

        // Image
        val image = context.imageOrNull(requiredOrientation)
        if (image != null) {
            imageView.configure(this, image, FIT_CENTER)
            imageView.setCornerRadius(imageCornerRadius.toFloat())
        } else {
            imageView.visibility = View.GONE
        }

        // TextView
        textView.configure(messageText.body)

        // CloseButton
        val closeButton = message.closeButton
        if (closeButton != null) {
            closeButtonView.configure(this, closeButton)
        } else {
            closeButtonView.visibility = View.GONE
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsetsCompat) {
        val inAppMessageAlignment = controller.context.message.layout.alignment?.vertical

        if(inAppMessageAlignment == InAppMessage.Message.Alignment.Vertical.TOP) {
            updateLayoutParams<MarginLayoutParams> {
                topMargin = this@InAppMessageBannerView.topMargin + insets.systemWindowInsetTop
            }

        } else if(inAppMessageAlignment == InAppMessage.Message.Alignment.Vertical.BOTTOM) {
            updateLayoutParams<MarginLayoutParams> {
                bottomMargin = this@InAppMessageBannerView.bottomMargin + insets.systemWindowInsetBottom
            }
        }
    }

    private val messageLayoutParams: FrameLayout.LayoutParams
        get() {
            val layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            layoutParams.width = maxWidth
            layoutParams.gravity = messageAlignment.bannerGravity
            layoutParams.setMargins(horizontalMargin, topMargin, horizontalMargin, bottomMargin)
            return layoutParams
        }

    private val messageBackground: Drawable
        get() {
            return Drawables.of(radii = CornerRadii.of(cornerRadius.toFloat()), color = message.backgroundColor)
        }

    companion object {
        fun create(activity: Activity): InAppMessageBannerView {
            @SuppressLint("InflateParams")
            val view = activity.layoutInflater.inflate(R.layout.hackle_iam_banner, null)
            return view as InAppMessageBannerView
        }
    }
}

internal val InAppMessage.Message.Alignment.bannerGravity: Int
    get() = when (vertical) {
        InAppMessage.Message.Alignment.Vertical.BOTTOM -> (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        InAppMessage.Message.Alignment.Vertical.MIDDLE -> Gravity.CENTER
        InAppMessage.Message.Alignment.Vertical.TOP -> (Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    }
