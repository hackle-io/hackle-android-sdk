package io.hackle.android.inappmessage.base

import io.hackle.android.Hackle
import io.hackle.android.app
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.InAppMessage

internal object InAppMessageTrack {

    fun impressionTrack(
        inAppMessage: InAppMessage,
        message: InAppMessage.MessageContext.Message,
        decisionReason: DecisionReason
    ) {
        val inAppMessageId = inAppMessage.id
        val inAppMessageKey = inAppMessage.key

        val propertiesBuilder = PropertiesBuilder()
            .add("in_app_message_id", inAppMessageId)
            .add("campaign_key", inAppMessageKey)
            .add("title_text", message.text?.title?.text)
            .add("body_text", message.text?.body?.text)
            .add("button_text", message.buttons.map { it.text })
            .add("image_url", message.images.map { it.imagePath } )
            .add("decision_reason", decisionReason.name)

        Hackle.app.track(
            Event.builder(IN_APP_IMPRESSION)
                .properties(propertiesBuilder.build())
                .build()
        )
    }

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
                    .add("in_app_message_id", inAppMessageId)
                    .add("campaign_key", inAppMessageKey)
                    .add("action_area", src.name)
                    .add("action_type", message.images[itemIdx].action?.type)
                    .add("button_text", "")
                    .add("action_value", message.images[itemIdx].imagePath)
            }

            ActionSource.BUTTON -> {
                PropertiesBuilder()
                    .add("in_app_message_id", inAppMessageId)
                    .add("campaign_key", inAppMessageKey)
                    .add("action_area", src.name)
                    .add("action_type", message.buttons[itemIdx].action.type)
                    .add("button_text", message.buttons[itemIdx].text)
                    .add("action_value", message.buttons[itemIdx].action.value)
            }

            ActionSource.X_BUTTON -> {
                PropertiesBuilder()
                    .add("in_app_message_id", inAppMessageId)
                    .add("campaign_key", inAppMessageKey)
                    .add("action_area", src.name)
                    .add("action_type", InAppMessage.MessageContext.Action.Type.CLOSE.name)
            }
        }

        Hackle.app.track(
            Event.Builder(IN_APP_ACTION)
                .properties(propertiesBuilder.build())
                .build()
        )
    }

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
