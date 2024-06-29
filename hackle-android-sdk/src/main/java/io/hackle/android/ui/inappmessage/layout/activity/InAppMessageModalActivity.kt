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
    private lateinit var frameView: RelativeLayout
    private lateinit var containerView: InAppMessageModalContainerView
    private lateinit var landContainerView: InAppMessageModalLandContainerView

    private lateinit var imageView: InAppMessageImageView
    private lateinit var textContainerView: InAppMessageTextContainerView
    private lateinit var firstButtonView: InAppMessageButtonView
    private lateinit var secondButtonView: InAppMessageButtonView

    private lateinit var leftBottomButtonView: InAppMessageButtonView
    private lateinit var rightBottomButtonView: InAppMessageButtonView

    private lateinit var closeButtonView: TextView

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

        // Frame & Container
        when (orientation) {
            VERTICAL -> {
                setContentView(R.layout.hackle_iam_modal)
                frameView = findViewById(R.id.hackle_iam_modal_frame_view)
                frameView.setOnClickListener { close() }

                imageView = findViewById(R.id.hackle_iam_modal_image_view)
                loadImage(orientation)

                containerView = findViewById(R.id.hackle_iam_modal_container_view)
                containerView.setModalFrameStyle(message)
                containerView.setImageViewStyle(message, imageView)
                containerView.setOnClickListener {
                    val action = message.action
                    if (action != null) {
                        handle(InAppMessageEvent.Action(action, MESSAGE))
                    }
                }
            }

            HORIZONTAL -> {
                setContentView(R.layout.hackle_iam_modal_land)
                frameView = findViewById(R.id.hackle_iam_modal_frame_view)
                frameView.setOnClickListener { close() }

                imageView = findViewById(R.id.hackle_iam_modal_image_view)
                loadImage(orientation)

                landContainerView = findViewById(R.id.hackle_iam_modal_land_container_view)
                landContainerView.setModalFrameStyle(message)
                landContainerView.setImageViewStyle(message, imageView)
                landContainerView.setOnClickListener {
                    val action = message.action
                    if (action != null) {
                        handle(InAppMessageEvent.Action(action, MESSAGE))
                    }
                }
            }
        }.safe

        // TextView
        textContainerView = findViewById(R.id.hackle_iam_modal_text_container_view)
        textContainerView.bind(message)

        // Buttons
        val buttonContainer = findViewById<View>(R.id.hackle_iam_modal_button_container_view)
        firstButtonView = findViewById(R.id.hackle_iam_modal_first_button)
        secondButtonView = findViewById(R.id.hackle_iam_modal_second_button)

        val radiusPx = cornerRadius.toFloat()
        when (message.buttons.size) {
            0 -> {
                buttonContainer.visibility = View.GONE
                imageView.setCornersRadius(radiusPx)
            }

            1 -> {
                buttonContainer.visibility = View.VISIBLE
                imageView.setCornersRadii(radiusPx, radiusPx, 0f, 0f)
                configureButton(firstButtonView, message.buttons[0])
                firstButtonView.layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT).apply { rightMargin = 0 }
            }

            2 -> {
                buttonContainer.visibility = View.VISIBLE
                imageView.setCornersRadii(radiusPx, radiusPx, 0f, 0f)
                configureButton(firstButtonView, message.buttons[0])
                configureButton(secondButtonView, message.buttons[1])
            }
        }
        imageView.setHeightRatio(0.8)

        // OuterButton
        leftBottomButtonView = findViewById(R.id.hackle_iam_modal_left_bottom_outer_button)
        rightBottomButtonView = findViewById(R.id.hackle_iam_modal_right_bottom_outer_button)
        if (message.layout.layoutType == InAppMessage.LayoutType.IMAGE) {
            message.outerButtonOrNull(LEFT, BOTTOM)?.let {
                configureButton(leftBottomButtonView, it)
            }
            message.outerButtonOrNull(RIGHT, BOTTOM)?.let {
                configureButton(rightBottomButtonView, it)
            }
        }

        // CloseButton
        closeButtonView = findViewById(R.id.hackle_iam_modal_close_button)
        message.closeButton?.let { button ->
            closeButtonView.setTextColor(button.textColor)
            closeButtonView.setOnClickListener {
                handle(InAppMessageEvent.Action(button.action, X_BUTTON))
            }
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

    private fun loadImage(orientation: InAppMessage.Orientation) {
        val image = context.image(orientation)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.render(image, this)

        imageView.setOnClickListener {
            val action = image.action
            if (action != null) {
                handle(InAppMessageEvent.Action(action, IMAGE))
            }
        }
    }
}
