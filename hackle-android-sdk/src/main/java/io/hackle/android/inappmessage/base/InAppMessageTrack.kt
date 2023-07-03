package io.hackle.android.inappmessage.base

import io.hackle.android.Hackle
import io.hackle.android.app
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.core.model.InAppMessage

object InAppMessageTrack {

    @JvmStatic
    fun impressionTrack(
        inAppMessage: InAppMessage
    ) {
        val inAppMessageId = inAppMessage.id
        val inAppMessageKey = inAppMessage.key
        val messageContext = inAppMessage.messageContext

        val propertiesBuilder = PropertiesBuilder()
            .add(
                mapOf(
                    "in_app_message_id" to inAppMessageId,
                    "campaign_key" to inAppMessageKey,
                    "title_text" to messageContext.messages.map {
                        it.text?.title?.text ?: ""
                    },
                    "body_text" to messageContext.messages.map { it.text?.body?.text ?: "" },
                    "button_text" to messageContext.messages.flatMap { message -> message.buttons.map { it.text } },
                    "image_url" to messageContext.messages.flatMap { message -> message.images.map { it.imagePath } }
                )
            )

        Hackle.app.track(
            Event.builder(IN_APP_IMPRESSION)
                .properties(propertiesBuilder.build())
                .build()
        )
    }

    @JvmStatic
    fun actionTrack(
        inAppMessageId: Long,
        inAppMessageKey: Long,
        message: InAppMessage.MessageContext.Message,
        src: ActionSource,
        itemIdx: Int = 0
    ) {

        val propertiesBuilder = when (src) {
            ActionSource.IMAGE -> {
                PropertiesBuilder()
                    .add(
                        mapOf(
                            "in_app_message_id" to inAppMessageId,
                            "campaign_key" to inAppMessageKey,
                            "action_area" to src.name,
                            "action_type" to message.images[itemIdx].action?.type,
                            "button_text" to "",
                            "action_value" to message.images[itemIdx].imagePath

                        )
                    )
            }

            ActionSource.BUTTON -> {
                PropertiesBuilder()
                    .add(
                        mapOf(
                            "in_app_message_id" to inAppMessageId,
                            "campaign_key" to inAppMessageKey,
                            "action_area" to src.name,
                            "action_type" to message.buttons[itemIdx].action.type,
                            "button_text" to message.buttons[itemIdx].text,
                            "action_value" to message.buttons[itemIdx].action.value
                        )
                    )
            }

            ActionSource.X_BUTTON -> {
                PropertiesBuilder()
                    .add(
                        mapOf(
                            "in_app_message_id" to inAppMessageId,
                            "campaign_key" to inAppMessageKey,
                            "action_area" to src.name,
                            "action_type" to InAppMessage.MessageContext.Action.Type.CLOSE
                        )
                    )
            }
        }

        Hackle.app.track(
            Event.Builder(IN_APP_ACTION)
                .properties(propertiesBuilder.build())
                .build()
        )
    }

    @JvmStatic
    fun closeTrack(inAppMessageId: Long, inAppMessageKey: Long) {
        val propertiesBuilder = PropertiesBuilder()
            .add("in_app_message_id", inAppMessageId)
            .add("campaign_key", inAppMessageKey)

        Hackle.app.track(
            Event.Builder(IN_APP_CLOSE)
                .properties(propertiesBuilder.build())
                .build()
        )
    }


    enum class ActionSource {
        IMAGE,
        BUTTON,
        X_BUTTON
    }

    private const val IN_APP_IMPRESSION = "\$in_app_impression"
    private const val IN_APP_ACTION = "\$in_app_action"
    private const val IN_APP_CLOSE = "\$in_app_close"

}
