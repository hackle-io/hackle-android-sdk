package io.hackle.android.ui.inappmessage.activity

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.RelativeLayout
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import io.hackle.android.R
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.HackleActivity
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.view.ActivityInAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageImageView
import io.hackle.android.ui.inappmessage.view.InAppMessageModalFrame
import io.hackle.android.ui.inappmessage.view.InAppMessageModalFrameLand
import io.hackle.android.ui.inappmessage.view.InAppMessageTextContainerView
import io.hackle.android.ui.inappmessage.view.handle
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionArea.BUTTON
import io.hackle.sdk.core.model.InAppMessage.ActionArea.IMAGE
import io.hackle.sdk.core.model.InAppMessage.ActionArea.X_BUTTON
import io.hackle.sdk.core.model.InAppMessage.Orientation.HORIZONTAL
import io.hackle.sdk.core.model.InAppMessage.Orientation.VERTICAL
import kotlin.math.roundToInt


internal class InAppMessageActivity : FragmentActivity(), HackleActivity {

    // View
    private lateinit var inAppMessageImage: InAppMessageImageView
    private lateinit var textContainerView: InAppMessageTextContainerView
    private lateinit var buttonOne: Button
    private lateinit var buttonTwo: Button
    private lateinit var modalFrame: InAppMessageModalFrame
    private lateinit var landModalFrame: InAppMessageModalFrameLand
    private lateinit var closeButton: ImageButton
    private lateinit var frame: RelativeLayout

    private lateinit var messageView: ActivityInAppMessageView

    // Model
    private val context: InAppMessagePresentationContext get() = messageView.context
    private val inAppMessage: InAppMessage get() = context.inAppMessage
    private val message: InAppMessage.Message get() = context.message
    private val messageContext: InAppMessage.MessageContext get() = inAppMessage.messageContext
    private val images: List<InAppMessage.Message.Image> get() = message.images

    private val currentOrientation get() = resources.configuration.orientation

    private val requestOptions: RequestOptions
        get() {
            // images are only cached for 1 minute
            return RequestOptions()
                .onlyRetrieveFromCache(false) // true is cached only set
                .signature(ObjectKey(System.currentTimeMillis() / (1000 * 60 * 1)))
        }


    // Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!initialize()) {
            finish()
            return
        }
        setFinishOnTouchOutside(true)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        layout()
    }

    private fun initialize(): Boolean {
        val messageView = InAppMessageUi.instance.currentMessageView ?: return false
        if (messageView !is ActivityInAppMessageView) {
            messageView.close()
            return false
        }

        val messageId = intent.getLongExtra(ActivityInAppMessageView.MESSAGE_ID, -1)
        if (messageView.context.inAppMessage.id != messageId) {
            messageView.close()
            return false
        }

        this.messageView = messageView
        messageView.onOpen(this)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        layout()
    }

    // Layout

    private fun layout() {
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if (!messageContext.orientations.contains(VERTICAL)) {
                close()
                return
            }

            setContentView(R.layout.hackle_iam_modal_text)
            frame = findViewById(R.id.hackle_inappmessage_modal_frame)
            frame.setOnClickListener { close() }
            modalFrame = findViewById(R.id.hackle_in_app_message_frame)
            inAppMessageImage = findViewById(R.id.hackle_in_app_message_image)
            loadImageWithGlide(VERTICAL)
            modalFrame.setModalFrameStyle(message)
            modalFrame.setImageViewStyle(message, inAppMessageImage)
            textContainerView = findViewById(R.id.hackle_in_app_message_text_container)
            textContainerView.bind(message)

        } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!messageContext.orientations.contains(HORIZONTAL)) {
                close()
                return
            }

            setContentView(R.layout.hackle_iam_modal_text_land)
            frame = findViewById(R.id.hackle_inappmessage_modal_frame)
            frame.setOnClickListener { close() }
            landModalFrame = findViewById(R.id.hackle_in_app_message_frame_land)
            inAppMessageImage = findViewById(R.id.hackle_in_app_message_image)
            loadImageWithGlide(HORIZONTAL)
            landModalFrame.setModalFrameStyle(message)
            landModalFrame.setImageViewStyle(message, inAppMessageImage)
            textContainerView = findViewById(R.id.hackle_in_app_message_text_container)
            textContainerView.bind(message)
        }
        setButtons()
    }

    private fun setButtons() {
        // Close Button
        closeButton = findViewById(R.id.hackle_in_app_close_button)
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

        // Buttons
        buttonOne = findViewById(R.id.hackle_in_app_button_one)
        buttonTwo = findViewById(R.id.hackle_in_app_button_two)

        if (message.buttons.size == 2) {
            configureButton(buttonOne, message.buttons[0])
            configureButton(buttonTwo, message.buttons[1])
        }

        if (message.buttons.size == 1) {
            configureButton(buttonOne, message.buttons[0])
            val layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT)
            layoutParams.rightMargin = changeToDp(0)
            buttonOne.layoutParams = layoutParams
        }
    }

    private fun configureButton(buttonView: Button, button: InAppMessage.Message.Button) {
        buttonView.visibility = Button.VISIBLE
        buttonView.text = button.text
        buttonView.setTextColor(Color.parseColor(button.style.textColor))
        buttonView.background = extractButtonBackground(button)
        buttonView.setOnClickListener {
            handle(InAppMessageEvent.Action(button.action, BUTTON, button.text))
        }
    }

    private fun extractButtonBackground(button: InAppMessage.Message.Button): GradientDrawable {
        val radius = changeToDp(8)

        val backgroundColor = Color.parseColor(button.style.bgColor)
        val boarderColor = Color.parseColor(button.style.borderColor)

        val border = GradientDrawable()
        border.setColor(backgroundColor)
        border.setStroke(1, boarderColor)
        border.cornerRadius = radius.toFloat()

        return border
    }

    private fun loadImageWithGlide(orientation: InAppMessage.Orientation) {
        val image = images.first { it.orientation == orientation }

        Glide.with(this)
            .load(image.imagePath)
            .apply(requestOptions)
            .fitCenter()
            .into(inAppMessageImage).also {
                image.action?.let { imageAction ->
                    inAppMessageImage.setOnClickListener {
                        handle(InAppMessageEvent.Action(imageAction, IMAGE))
                    }
                }
            }
    }

    private fun changeToDp(pixel: Int): Int {
        val displayMetrics = resources.displayMetrics
        return (pixel * displayMetrics.density).roundToInt()
    }

    // Interaction

    private fun close() {
        messageView.close()
    }

    private fun handle(event: InAppMessageEvent.Action) {
        messageView.handle(event)
    }
}
