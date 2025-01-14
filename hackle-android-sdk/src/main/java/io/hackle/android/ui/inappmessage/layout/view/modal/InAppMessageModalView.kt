package io.hackle.android.ui.inappmessage.layout.view.modal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import io.hackle.android.R
import io.hackle.android.ui.core.Animations
import io.hackle.android.ui.core.CornerRadii
import io.hackle.android.ui.core.Drawables
import io.hackle.android.ui.inappmessage.*
import io.hackle.android.ui.inappmessage.layout.InAppMessageAnimator
import io.hackle.android.ui.inappmessage.layout.view.*
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageImageView.AspectRatio
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.LayoutType.*
import io.hackle.sdk.core.model.InAppMessage.Orientation.HORIZONTAL
import io.hackle.sdk.core.model.InAppMessage.Orientation.VERTICAL

internal class InAppMessageModalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : InAppMessageView(context, attrs, defStyleAttr) {

    // Attribute
    private val cornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_corner_radius)
    private val buttonStrokeWith get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_button_stroke_width)
    private val buttonCornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_button_corner_radius)

    // View
    private val frameView: RelativeLayout get() = findViewById(R.id.hackle_iam_modal_frame_view)
    private val contentView: InAppMessageModalContentView get() = findViewById(R.id.hackle_iam_banner_content_view)
    private val containerView: InAppMessageModalContainerView get() = findViewById(R.id.hackle_iam_modal_container_view)
    private val closeButtonView: InAppMessageCloseButtonView get() = findViewById(R.id.hackle_iam_modal_close_button_view)
    private val imageContainerView: InAppMessageImageContainerView get() = findViewById(R.id.hackle_iam_modal_image_container_view)
    private val textContainerView: LinearLayout get() = findViewById(R.id.hackle_iam_modal_text_container_view)
    private val titleTextView: InAppMessageTextView get() = findViewById(R.id.hackle_iam_modal_title_text_view)
    private val bodyTextView: InAppMessageTextView get() = findViewById(R.id.hackle_iam_modal_body_text_view)
    private val buttonContainerView: LinearLayout get() = findViewById(R.id.hackle_iam_modal_button_container_view)
    private val firstButtonView: InAppMessageButtonView get() = findViewById(R.id.hackle_iam_modal_first_button)
    private val secondButtonView: InAppMessageButtonView get() = findViewById(R.id.hackle_iam_modal_second_button)
    private val outerButtonContainerView: RelativeLayout get() = findViewById(R.id.hackle_iam_modal_bottom_outer_button_container_view)
    private val leftBottomButtonView: InAppMessagePositionalButtonView get() = findViewById(R.id.hackle_iam_modal_left_bottom_outer_button)
    private val rightBottomButtonView: InAppMessagePositionalButtonView get() = findViewById(R.id.hackle_iam_modal_right_bottom_outer_button)

    private val buttonViews get() = listOf(firstButtonView, secondButtonView)
    private val outerButtonViews get() = listOf(leftBottomButtonView, rightBottomButtonView)

    // Animation
    override val openAnimator: InAppMessageAnimator get() = InAppMessageAnimator.of(this, Animations.fadeIn(100))
    override val closeAnimator: InAppMessageAnimator get() = InAppMessageAnimator.of(this, Animations.fadeOut(100))

    // Configuration
    override fun configure() {
        val configuration = configuration

        // FrameView (outside of modal)
        frameView.setOnClickListener(createCloseListener())

        // ContentView (content area)
        contentView.setMaxWidthRatio(widthRatio)

        // ContainerView (inside of modal)
        containerView.background = messageBackground
        containerView.setCornerRadii(cornerRadii)
        containerView.setOnClickListener(createMessageClickListener())

        // Image
        imageContainerView.setAspectRatio(imageAspectRatio)
        imageContainerView.configure(this, requiredOrientation)

        // Text
        if (configuration.text) {
            val messageText = requireNotNull(message.text) { "Not found in-app message text [${inAppMessage.id}]" }
            titleTextView.configure(messageText.title)
            bodyTextView.configure(messageText.body)
        } else {
            textContainerView.visibility = View.GONE
        }

        // Button
        if (configuration.button) {
            for ((index, buttonView) in buttonViews.withIndex()) {
                val button = message.buttons.getOrNull(index)
                if (button != null) {
                    buttonView.configure(this, button, button.backgroundDrawable)
                } else {
                    buttonView.visibility = View.GONE
                }
            }
        } else {
            buttonContainerView.visibility = View.GONE
        }

        // OuterButton
        if (configuration.outerButton) {
            for (outerButtonView in outerButtonViews) {
                val outerButton = message.outerButtonOrNull(outerButtonView.alignment)
                if (outerButton != null) {
                    outerButtonView.configure(this, outerButton.button, ColorDrawable(Color.TRANSPARENT))
                } else {
                    outerButtonView.visibility = View.GONE
                }
            }
        } else {
            outerButtonContainerView.visibility = View.GONE
        }

        // CloseButton
        val closeButton = message.closeButton
        if (context.images(requiredOrientation).size < 2 && closeButton != null) {
            closeButtonView.configure(this, closeButton)
        } else {
            closeButtonView.visibility = View.GONE
        }
    }

    private class Configuration(
        val image: Boolean,
        val text: Boolean,
        val button: Boolean,
        val outerButton: Boolean,
    )

    private val widthRatio: Double
        get() {
            return when (requiredOrientation) {
                VERTICAL -> 1.0
                HORIZONTAL -> 0.8
            }
        }

    private val configuration: Configuration
        get() = when (message.layout.layoutType) {
            NONE -> Configuration(image = false, text = false, button = false, outerButton = false)
            IMAGE_ONLY -> Configuration(image = true, text = false, button = true, outerButton = false)
            IMAGE_TEXT -> Configuration(image = true, text = true, button = true, outerButton = false)
            TEXT_ONLY -> Configuration(image = false, text = true, button = true, outerButton = false)
            IMAGE -> Configuration(image = true, text = false, button = false, outerButton = true)
        }

    private val cornerRadii: CornerRadii get() = CornerRadii.of(cornerRadius.toFloat())
    private val messageBackground: Drawable
        get() {
            return when (message.layout.layoutType) {
                NONE -> Drawables.transparent()
                IMAGE -> Drawables.transparent()
                IMAGE_ONLY, IMAGE_TEXT, TEXT_ONLY -> Drawables.of(radii = cornerRadii, color = message.backgroundColor)
            }
        }

    private val imageAspectRatio: AspectRatio?
        get() {
            return when (message.layout.layoutType) {
                NONE -> null
                TEXT_ONLY -> null
                IMAGE_TEXT -> AspectRatio(width = 290f, height = 100f)
                IMAGE_ONLY, IMAGE -> {
                    when (requiredOrientation) {
                        VERTICAL -> AspectRatio(width = 200f, height = 300f)
                        HORIZONTAL -> AspectRatio(width = 300f, height = 200f)
                    }
                }
            }
        }

    private val InAppMessage.Message.Button.backgroundDrawable: Drawable
        get() {
            val background = Drawables.of(radii = CornerRadii.of(buttonCornerRadius.toFloat()), color = backgroundColor)
            background.setStroke(buttonStrokeWith, borderColor)
            return background
        }

    companion object {
        fun create(activity: Activity): InAppMessageModalView {
            @SuppressLint("InflateParams")
            val view = activity.layoutInflater.inflate(R.layout.hackle_iam_modal, null)
            return view as InAppMessageModalView
        }
    }
}
