package io.hackle.android.ui.inappmessage.layout.view.sheet

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import io.hackle.android.R
import io.hackle.android.ui.core.Animations
import io.hackle.android.ui.core.CornerRadii
import io.hackle.android.ui.core.Drawables
import io.hackle.android.ui.inappmessage.backgroundColor
import io.hackle.android.ui.inappmessage.buttonOrNull
import io.hackle.android.ui.inappmessage.images
import io.hackle.android.ui.inappmessage.layout.InAppMessageAnimator
import io.hackle.android.ui.inappmessage.layout.view.*
import io.hackle.android.ui.inappmessage.requiredOrientation

internal class InAppMessageBottomSheetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : InAppMessageView(context, attrs, defStyleAttr) {

    // Attribute
    private val cornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_bottom_sheet_radius)

    // View
    private val frameView: RelativeLayout get() = findViewById(R.id.hackle_iam_bottom_sheet_frame_view)
    private val containerView: InAppMessageBottomSheetContainerView get() = findViewById(R.id.hackle_iam_bottom_sheet_container_view)
    private val closeButtonView: InAppMessageCloseButtonView get() = findViewById(R.id.hackle_iam_bottom_sheet_close_button_view)
    private val imageContainerView: InAppMessageImageContainerView get() = findViewById(R.id.hackle_iam_bottom_sheet_image_container_view)
    private val buttonContainerView: RelativeLayout get() = findViewById(R.id.hackle_iam_bottom_sheet_button_container_view)
    private val leftButtonView: InAppMessagePositionalButtonView get() = findViewById(R.id.hackle_iam_bottom_sheet_left_bottom_button)
    private val rightButtonView: InAppMessagePositionalButtonView get() = findViewById(R.id.hackle_iam_bottom_sheet_right_bottom_button)

    private val buttonViews get() = listOf(leftButtonView, rightButtonView)

    // Animation
    override val openAnimator: InAppMessageAnimator
        get() {
            val fadeIn = InAppMessageAnimator.of(frameView, Animations.fadeIn(300))
            val slideIn = InAppMessageAnimator.of(containerView, Animations.slideInBottom(300))
            return InAppMessageAnimator.of(fadeIn, slideIn)
        }
    override val closeAnimator: InAppMessageAnimator
        get() {
            val fadeOut = InAppMessageAnimator.of(frameView, Animations.fadeOut(300))
            val slideOut = InAppMessageAnimator.of(containerView, Animations.slideOutBottom(300))
            return InAppMessageAnimator.of(fadeOut, slideOut)
        }

    // Configuration
    override fun configure() {
        // FrameView (outside of bottom sheet)
        frameView.setOnClickListener(createCloseListener())

        // ContainerView (inside of bottom sheet)
        containerView.background = messageBackground
        containerView.setCornerRadii(cornerRadii)
        containerView.setOnClickListener(createMessageClickListener())

        // Image
        imageContainerView.setAspectRatio(imageAspectRatio)
        imageContainerView.configure(this, requiredOrientation)

        // Button
        if (message.innerButtons.isNotEmpty()) {
            for (buttonView in buttonViews) {
                val button = message.buttonOrNull(buttonView.alignment)
                if (button != null) {
                    buttonView.configure(this, button.button, Drawables.transparent())
                } else {
                    buttonView.visibility = View.GONE
                }
            }
        } else {
            buttonContainerView.visibility = View.GONE
        }

        // CloseButton
        val closeButton = message.closeButton
        if (context.images(requiredOrientation).size < 2 && closeButton != null) {
            closeButtonView.configure(this, closeButton)
        } else {
            closeButtonView.visibility = View.GONE
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsetsCompat) {
        updateLayoutParams<MarginLayoutParams> {
            bottomMargin = insets.systemWindowInsetBottom
        }
    }

    private val cornerRadii: CornerRadii
        get() = CornerRadii.of(
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            0f,
            0f
        )
    private val messageBackground: Drawable
        get() = Drawables.of(
            radii = cornerRadii,
            color = message.backgroundColor
        )

    private val imageAspectRatio
        get() = InAppMessageImageView.AspectRatio(
            width = 300f,
            height = 200f
        )

    companion object {
        fun create(activity: Activity): InAppMessageBottomSheetView {
            @SuppressLint("InflateParams")
            val view = activity.layoutInflater.inflate(R.layout.hackle_iam_bottom_sheet, null)
            return view as InAppMessageBottomSheetView
        }
    }
}
