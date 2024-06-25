package io.hackle.android.ui.inappmessage.layout.activity

import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.RelativeLayout
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import io.hackle.android.R
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.layout.close
import io.hackle.android.ui.inappmessage.layout.handle
import io.hackle.android.ui.inappmessage.layout.view.*
import io.hackle.android.ui.inappmessage.orientation
import io.hackle.android.ui.inappmessage.outerButtonOrNull
import io.hackle.android.ui.inappmessage.px
import io.hackle.android.ui.inappmessage.supports
import io.hackle.sdk.core.internal.utils.safe
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionArea.*
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Horizontal.LEFT
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Horizontal.RIGHT
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Vertical.BOTTOM
import io.hackle.sdk.core.model.InAppMessage.Orientation.HORIZONTAL
import io.hackle.sdk.core.model.InAppMessage.Orientation.VERTICAL


internal class InAppMessageModalActivity : InAppMessageActivity() {

    private lateinit var frameView: RelativeLayout
    private lateinit var containerView: InAppMessageModalContainerView
    private lateinit var landContainerView: InAppMessageModalLandContainerView

    private lateinit var imageView: InAppMessageImageView
    private lateinit var textContainerView: InAppMessageTextContainerView
    private lateinit var firstButton: InAppMessageButtonView
    private lateinit var secondButton: InAppMessageButtonView

    private lateinit var leftBottomButton: InAppMessageButtonView
    private lateinit var rightBottomButton: InAppMessageButtonView

    private lateinit var closeButton: ImageButton

    private val requestOptions: RequestOptions
        get() {
            // images are only cached for 1 minute
            return RequestOptions()
                .onlyRetrieveFromCache(false) // true is cached only set
                .signature(ObjectKey(System.currentTimeMillis() / (1000 * 60 * 1)))
        }

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
                loadImageWithGlide(orientation)
                containerView = findViewById(R.id.hackle_iam_modal_container_view)
                containerView.setModalFrameStyle(message)
                containerView.setImageViewStyle(message, imageView)
            }

            HORIZONTAL -> {
                setContentView(R.layout.hackle_iam_modal_land)
                frameView = findViewById(R.id.hackle_iam_modal_frame_view)
                frameView.setOnClickListener { close() }
                landContainerView = findViewById(R.id.hackle_iam_modal_land_container_view)
                imageView = findViewById(R.id.hackle_iam_modal_image_view)
                loadImageWithGlide(orientation)
                landContainerView.setModalFrameStyle(message)
                landContainerView.setImageViewStyle(message, imageView)
            }
        }.safe

        // TextView
        textContainerView = findViewById(R.id.hackle_iam_modal_text_container_view)
        textContainerView.bind(message)

        // Buttons
        val buttonContainer = findViewById<View>(R.id.hackle_iam_modal_button_container_view)
        firstButton = findViewById(R.id.hackle_iam_modal_first_button)
        secondButton = findViewById(R.id.hackle_iam_modal_second_button)

        val radiusPx = px(8)
        when (message.buttons.size) {
            0 -> {
                buttonContainer.visibility = View.GONE
                imageView.setCornersRadiiPx(radiusPx)
            }

            1 -> {
                buttonContainer.visibility = View.VISIBLE
                imageView.setCornersRadiiPx(radiusPx, radiusPx, 0f, 0f)
                configureButton(firstButton, message.buttons[0])
                firstButton.layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT).apply { rightMargin = 0 }
            }

            2 -> {
                buttonContainer.visibility = View.VISIBLE
                imageView.setCornersRadiiPx(radiusPx, radiusPx, 0f, 0f)
                configureButton(firstButton, message.buttons[0])
                configureButton(secondButton, message.buttons[1])
            }
        }

        // OuterButton
        leftBottomButton = findViewById(R.id.hackle_iam_modal_left_bottom_outer_button)
        rightBottomButton = findViewById(R.id.hackle_iam_modal_right_bottom_outer_button)
        if (message.layout.layoutType == InAppMessage.LayoutType.IMAGE) {
            message.outerButtonOrNull(LEFT, BOTTOM)?.let {
                configureButton(leftBottomButton, it)
            }

            message.outerButtonOrNull(RIGHT, BOTTOM)?.let {
                configureButton(rightBottomButton, it)
            }
        }

        // CloseButton
        closeButton = findViewById(R.id.hackle_iam_modal_close_button)
        message.closeButton?.let { button ->
            val buttonDrawable = closeButton.drawable
            buttonDrawable.mutate()
            buttonDrawable.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    Color.parseColor(button.style.textColor),
                    BlendModeCompat.SRC_IN
                )
            closeButton.setImageDrawable(buttonDrawable)

            closeButton.setOnClickListener {
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

    private fun loadImageWithGlide(orientation: InAppMessage.Orientation) {
        val image = images.first { it.orientation == orientation }

        Glide.with(this)
            .load(image.imagePath)
            .apply(requestOptions)
            .fitCenter()
            .into(imageView).also {
                image.action?.let { imageAction ->
                    imageView.setOnClickListener {
                        handle(InAppMessageEvent.Action(imageAction, IMAGE))
                    }
                }
            }
    }
}
