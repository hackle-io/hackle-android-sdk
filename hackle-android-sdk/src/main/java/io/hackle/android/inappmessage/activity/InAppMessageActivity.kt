package io.hackle.android.inappmessage.activity

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import io.hackle.android.HackleActivity
import io.hackle.android.R
import io.hackle.android.inappmessage.InAppMessageRenderer
import io.hackle.android.inappmessage.base.InAppMessageTrack
import io.hackle.android.inappmessage.base.InAppMessageTrack.actionTrack
import io.hackle.android.inappmessage.base.InAppMessageTrack.closeTrack
import io.hackle.android.inappmessage.storage.HackleInAppMessageStorageImpl
import io.hackle.android.inappmessage.storage.HackleInAppMessageStorageImpl.Companion.NEXT_24_HOUR_MILLISECONDS
import io.hackle.android.inappmessage.view.InAppMessageImageView
import io.hackle.android.inappmessage.view.InAppMessageModalFrame
import io.hackle.android.inappmessage.view.InAppMessageModalFrameLand
import io.hackle.android.inappmessage.view.InAppMessageTextContainerView
import io.hackle.android.internal.utils.parseJson
import io.hackle.sdk.core.HackleCoreContext
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.MessageContext.Action.Behavior.CLICK
import io.hackle.sdk.core.model.InAppMessage.MessageContext.Action.Type.CLOSE
import io.hackle.sdk.core.model.InAppMessage.MessageContext.Action.Type.HIDDEN
import io.hackle.sdk.core.model.InAppMessage.MessageContext.Action.Type.WEB_LINK
import io.hackle.sdk.core.model.InAppMessage.MessageContext.Message
import io.hackle.sdk.core.model.InAppMessage.MessageContext.Orientation.HORIZONTAL
import io.hackle.sdk.core.model.InAppMessage.MessageContext.Orientation.VERTICAL
import kotlin.math.roundToInt


internal class InAppMessageActivity : FragmentActivity(), HackleActivity {

    private lateinit var inAppMessageImage: InAppMessageImageView

    private lateinit var textContainerView: InAppMessageTextContainerView

    private lateinit var buttonOne: Button

    private lateinit var buttonTwo: Button


    private lateinit var modalFrame: InAppMessageModalFrame

    private lateinit var landModalFrame: InAppMessageModalFrameLand

    private lateinit var closeButton: ImageButton

    private lateinit var frame: RelativeLayout

    private lateinit var inAppMessage: InAppMessage

    private lateinit var images: List<Message.Image>

    private lateinit var messageContext: InAppMessage.MessageContext

    private lateinit var message: Message


    private val currentOrientation
        get() = resources.configuration.orientation

    private val requestOptions: RequestOptions
        get() {
            // images are only cached for 1 minute
            return RequestOptions()
                .onlyRetrieveFromCache(false) // true is cached only set
                .signature(ObjectKey(System.currentTimeMillis() / (1000 * 60 * 1)))
        }

    private fun initialize(): Boolean {
        inAppMessage =
            intent.extras?.getString("inAppMessage")?.parseJson<InAppMessage>() ?: let {
                log.error { "Could not find iam from intent." }
                return false
            }


        messageContext = inAppMessage.messageContext
        message = intent.extras?.getString("message")?.parseJson<Message>() ?: let {
            log.error { "Could not find message from intent." }
            return false
        }
        images = message.images

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!initialize()) {
            return
        }
        setFinishOnTouchOutside(true)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if (!messageContext.orientations.contains(VERTICAL)) {
                finish()
                return
            }

            setContentView(R.layout.hackle_iam_modal_text)
            frame = findViewById(R.id.hackle_inappmessage_modal_frame)
            frame.setOnClickListener { finish() }
            modalFrame = findViewById(R.id.hackle_in_app_message_frame)
            inAppMessageImage = findViewById(R.id.hackle_in_app_message_image)
            loadImageWithGlide(VERTICAL)
            modalFrame.setModalFrameStyle(message)
            modalFrame.setImageViewStyle(message, inAppMessageImage)
            textContainerView = findViewById(R.id.hackle_in_app_message_text_container)
            textContainerView.bind(message)

        } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (!messageContext.orientations.contains(HORIZONTAL)) {
                finish()
                return
            }

            setContentView(R.layout.hackle_iam_modal_text_land)
            frame = findViewById(R.id.hackle_inappmessage_modal_frame)
            frame.setOnClickListener { finish() }
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


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if (!messageContext.orientations.contains(VERTICAL)) {
                finish()
                return
            }

            setContentView(R.layout.hackle_iam_modal_text)
            frame = findViewById(R.id.hackle_inappmessage_modal_frame)
            frame.setOnClickListener { finish() }
            modalFrame = findViewById(R.id.hackle_in_app_message_frame)
            inAppMessageImage = findViewById(R.id.hackle_in_app_message_image)
            loadImageWithGlide(VERTICAL)
            modalFrame.setModalFrameStyle(message)
            modalFrame.setImageViewStyle(message, inAppMessageImage)
            textContainerView = findViewById(R.id.hackle_in_app_message_text_container)
            textContainerView.bind(message)

        } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!messageContext.orientations.contains(HORIZONTAL)) {
                finish()
                return
            }

            setContentView(R.layout.hackle_iam_modal_text_land)
            frame = findViewById(R.id.hackle_inappmessage_modal_frame)
            frame.setOnClickListener { finish() }
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
        closeButton = findViewById(R.id.hackle_in_app_close_button)

        if (message.closeButton != null) {
            val buttonDrawable = closeButton.drawable
            buttonDrawable.mutate()
            buttonDrawable.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    Color.parseColor(message.closeButton!!.style.color),
                    BlendModeCompat.SRC_IN
                )
            closeButton.setImageDrawable(buttonDrawable)
        }

        buttonOne = findViewById(R.id.hackle_in_app_button_one)
        buttonTwo = findViewById(R.id.hackle_in_app_button_two)

        if (message.buttons.size == 2) {
            buttonOne.visibility = Button.VISIBLE
            buttonOne.text = message.buttons[0].text
            buttonOne.setTextColor(Color.parseColor(message.buttons[0].style.textColor))
            buttonOne.background = extractButtonBackground(message.buttons[0])
            buttonOne.setOnClickListener {
                onButtonClick(buttonIdx = 0)
            }

            buttonTwo.visibility = Button.VISIBLE
            buttonTwo.text = message.buttons[1].text
            buttonTwo.setTextColor(Color.parseColor(message.buttons[1].style.textColor))
            buttonTwo.background = extractButtonBackground(message.buttons[1])
            buttonTwo.setOnClickListener {
                onButtonClick(buttonIdx = 1)
            }
        }

        if (message.buttons.size == 1) {
            val layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.rightMargin = changeToDp(0)
            buttonOne.visibility = Button.VISIBLE
            buttonOne.text = message.buttons[0].text
            buttonOne.setTextColor(Color.parseColor(message.buttons[0].style.textColor))
            buttonOne.background = extractButtonBackground(message.buttons[0])
            buttonOne.setOnClickListener {
                onButtonClick(buttonIdx = 0)
            }
            buttonOne.layoutParams = layoutParams
        }


        closeButton.setOnClickListener {
            onXButtonClick()
        }

    }

    private fun extractButtonBackground(button: Message.Button): GradientDrawable {
        val radius = changeToDp(8)

        val backgroundColor = Color.parseColor(button.style.bgColor)
        val boarderColor = Color.parseColor(button.style.borderColor)

        val border = GradientDrawable()
        border.setColor(backgroundColor)
        border.setStroke(1, boarderColor)
        border.cornerRadius = radius.toFloat()

        return border
    }

    private fun loadImageWithGlide(
        orientation: InAppMessage.MessageContext.Orientation
    ) {
        val image = images.first { it.orientation == orientation }

        Glide.with(this)
            .load(image.imagePath)
            .error(R.drawable.hackle_in_app_image_blank)
            .apply(requestOptions)
            .fitCenter()
            .into(inAppMessageImage).also {
                if (image.action != null) {
                    inAppMessageImage.setOnClickListener {
                        onImageClick(
                            image.action!!,
                            0
                        )
                    }
                }
            }
    }


    private fun onImageClick(
        action: InAppMessage.MessageContext.Action,
        imageIdx: Int,
    ) {
        if (action.behavior == CLICK) {
            when (action.type) {
                WEB_LINK -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(action.value)
                    try {
                        startActivity(intent)
                        actionTrack(
                            inAppMessage.id,
                            inAppMessage.key,
                            message,
                            InAppMessageTrack.ActionSource.IMAGE,
                            imageIdx
                        )
                    } catch (e: Exception) {
                        log.error { "On Image Click intent parse error: ${intent.data}" }
                        finish()
                    }
                }

                HIDDEN, CLOSE -> {
                    log.warn { "Invalid action Type : ${action.type}" }
                    finish()
                }
            }
        }
    }

    private fun onButtonClick(buttonIdx: Int) {
        val action = message.buttons[buttonIdx].action
        if (action.behavior == CLICK) {
            when (action.type) {
                CLOSE -> {
                    actionTrack(
                        inAppMessage.id,
                        inAppMessage.key,
                        message,
                        InAppMessageTrack.ActionSource.BUTTON,
                        buttonIdx
                    )
                    finish()
                }

                WEB_LINK -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(action.value)
                    try {
                        startActivity(intent)

                        actionTrack(
                            inAppMessage.id,
                            inAppMessage.key,
                            message,
                            InAppMessageTrack.ActionSource.BUTTON,
                            buttonIdx
                        )
                    } catch (e: Exception) {
                        log.error { "No Activity found to handle Intent : ${intent.data}" }
                        finish()
                    }
                }

                HIDDEN -> {
                    val storage = HackleCoreContext.get(HackleInAppMessageStorageImpl::class.java)
                    storage.put(
                        inAppMessage,
                        Clock.SYSTEM.currentMillis() + NEXT_24_HOUR_MILLISECONDS
                    )

                    actionTrack(
                        inAppMessage.id,
                        inAppMessage.key,
                        message,
                        InAppMessageTrack.ActionSource.BUTTON,
                        buttonIdx
                    )
                    finish()
                }
            }
        }
    }

    private fun onXButtonClick() {
        actionTrack(
            inAppMessage.id,
            inAppMessage.key,
            message,
            src = InAppMessageTrack.ActionSource.X_BUTTON,
            itemIdx = 0
        )
        finish()
    }

    override fun onDestroy() {
        closeTrack(inAppMessage.id, inAppMessage.key)
        super.onDestroy()
    }


    private fun changeToDp(pixel: Int): Int {
        val displayMetrics = resources.displayMetrics
        return (pixel * displayMetrics.density).roundToInt()
    }

    companion object {
        private val log = Logger<InAppMessageRenderer>()
    }
}