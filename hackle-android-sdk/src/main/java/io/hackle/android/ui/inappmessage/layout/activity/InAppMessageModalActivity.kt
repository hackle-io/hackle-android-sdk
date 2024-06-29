package io.hackle.android.ui.inappmessage.layout.activity

import android.content.res.Configuration
import android.view.View
import android.widget.*
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import io.hackle.android.R
import io.hackle.android.ui.inappmessage.*
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.layout.handle
import io.hackle.android.ui.inappmessage.layout.view.*
import io.hackle.sdk.core.internal.utils.safe
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionArea.*
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Horizontal.LEFT
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Horizontal.RIGHT
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Vertical.BOTTOM
import io.hackle.sdk.core.model.InAppMessage.Orientation.HORIZONTAL
import io.hackle.sdk.core.model.InAppMessage.Orientation.VERTICAL

internal class InAppMessageModalActivity : InAppMessageActivity() {

    // Attribute
    private val cornerRadius get() = resources.getDimensionPixelSize(R.dimen.hackle_iam_modal_corner_radius)

    // View
    private val frameView: RelativeLayout get() = findViewById(R.id.hackle_iam_modal_frame_view)
    private val containerView: InAppMessageModalContainerView
        get() = when (orientation) {
            VERTICAL -> findViewById(R.id.hackle_iam_modal_container_view)
            HORIZONTAL -> findViewById(R.id.hackle_iam_modal_land_container_view)
            null -> throw IllegalStateException("orientation is null")
        }
    private val imageView: InAppMessageImageView get() = findViewById(R.id.hackle_iam_modal_image_view)
    private val textContainerView: InAppMessageTextContainerView get() = findViewById(R.id.hackle_iam_modal_text_container_view)
    private val buttonContainerView: LinearLayout get() = findViewById(R.id.hackle_iam_modal_button_container_view)
    private val firstButtonView: InAppMessageButtonView get() = findViewById(R.id.hackle_iam_modal_first_button)
    private val secondButtonView: InAppMessageButtonView get() = findViewById(R.id.hackle_iam_modal_second_button)
    private val leftBottomButtonView: InAppMessageButtonView get() = findViewById(R.id.hackle_iam_modal_left_bottom_outer_button)
    private val rightBottomButtonView: InAppMessageButtonView get() = findViewById(R.id.hackle_iam_modal_right_bottom_outer_button)
    private val closeButtonView: TextView get() = findViewById(R.id.hackle_iam_modal_close_button)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        layout()
    }

    override fun onBackPressed() {
        close()
    }

    override fun layout() {
        val orientation = orientation
        if (orientation == null || !inAppMessage.supports(orientation)) {
            close()
            return
        }

        when (orientation) {
            VERTICAL -> setContentView(R.layout.hackle_iam_modal)
            HORIZONTAL -> setContentView(R.layout.hackle_iam_modal_land)
        }.safe

        // FrameView
        frameView.setOnClickListener { close() }

        // ContainerView
        containerView.setModalFrameStyle(message)
        containerView.setImageViewStyle(message, imageView)
        containerView.setOnClickListener {
            val action = message.action
            if (action != null) {
                handle(InAppMessageEvent.Action(action, MESSAGE))
            }
        }

        // ImageView
        val image = context.image(orientation)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.render(image, this)
        imageView.setOnClickListener {
            val action = image.action
            if (action != null) {
                handle(InAppMessageEvent.Action(action, IMAGE))
            }
        }

        // TextView
        textContainerView.bind(message)

        // Button
        val radiusPx = cornerRadius.toFloat()
        when (message.buttons.size) {
            0 -> {
                buttonContainerView.visibility = View.GONE
                imageView.setCornersRadius(radiusPx)
            }

            1 -> {
                buttonContainerView.visibility = View.VISIBLE
                imageView.setCornersRadii(radiusPx, radiusPx, 0f, 0f)
                configureButton(firstButtonView, message.buttons[0])
                firstButtonView.layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT).apply { rightMargin = 0 }
            }

            2 -> {
                buttonContainerView.visibility = View.VISIBLE
                imageView.setCornersRadii(radiusPx, radiusPx, 0f, 0f)
                configureButton(firstButtonView, message.buttons[0])
                configureButton(secondButtonView, message.buttons[1])
            }
        }
        imageView.setHeightRatio(0.8)

        // OuterButton
        if (message.layout.layoutType == InAppMessage.LayoutType.IMAGE) {
            message.outerButtonOrNull(LEFT, BOTTOM)?.let {
                configureButton(leftBottomButtonView, it)
            }
            message.outerButtonOrNull(RIGHT, BOTTOM)?.let {
                configureButton(rightBottomButtonView, it)
            }
        }

        // CloseButton
        val closeButton = message.closeButton
        if (closeButton != null) {
            closeButtonView.setTextColor(closeButton.textColor)
            closeButtonView.setOnClickListener {
                handle(InAppMessageEvent.Action(closeButton.action, X_BUTTON))
            }
        } else {
            closeButtonView.visibility = View.GONE
        }
    }

    private fun configureButton(buttonView: InAppMessageButtonView, button: InAppMessage.Message.Button) {
        buttonView.bind(button)
        buttonView.setOnClickListener {
            handle(InAppMessageEvent.Action(button.action, BUTTON, button.text))
        }
        buttonView.visibility = Button.VISIBLE
    }

    private fun configureButton(buttonView: InAppMessageButtonView, button: InAppMessage.Message.PositionalButton) {
        buttonView.bind(button)
        buttonView.setOnClickListener {
            handle(InAppMessageEvent.Action(button.button.action, BUTTON, button.button.text))
        }
        buttonView.visibility = Button.VISIBLE
    }
}
