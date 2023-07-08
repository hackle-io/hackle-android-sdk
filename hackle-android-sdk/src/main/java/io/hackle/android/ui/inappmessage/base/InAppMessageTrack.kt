package io.hackle.android.ui.inappmessage.base

import io.hackle.android.Hackle
import io.hackle.android.app
import io.hackle.android.internal.inappmessage.InAppMessageRenderSource
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.core.model.InAppMessage

internal object InAppMessageTrack {

    fun impressionTrack(source: InAppMessageRenderSource) {
        val inAppMessageId = source.inAppMessage.id
        val inAppMessageKey = source.inAppMessage.key

        val event = Event.builder(IN_APP_IMPRESSION)
            .property("in_app_message_id", inAppMessageId)
            .property("in_app_message_key", inAppMessageKey)
            .property("title_text", source.message.text?.title?.text)
            .property("body_text", source.message.text?.body?.text)
            .property("button_text", source.message.buttons.map { it.text })
            .property("image_url", source.message.images.map { it.imagePath })
            .properties(source.properties)
            .build()

        Hackle.app.track(event)
    }

    fun actionTrack(
        inAppMessageId: Long,
        inAppMessageKey: Long,
        message: InAppMessage.MessageContext.Message,
        src: ActionSource,
        itemIdx: Int = 0,
    ) {

        val propertiesBuilder = when (src) {
            ActionSource.IMAGE -> {
                PropertiesBuilder()
                    .add("in_app_message_id", inAppMessageId)
                    .add("in_app_message_key", inAppMessageKey)
                    .add("action_area", src.name)
                    .add("action_type", message.images[itemIdx].action?.type)
                    .add("button_text", "")
                    .add("action_value", message.images[itemIdx].imagePath)
            }

            ActionSource.BUTTON -> {
                PropertiesBuilder()
                    .add("in_app_message_id", inAppMessageId)
                    .add("in_app_message_key", inAppMessageKey)
                    .add("action_area", src.name)
                    .add("action_type", message.buttons[itemIdx].action.type)
                    .add("button_text", message.buttons[itemIdx].text)
                    .add("action_value", message.buttons[itemIdx].action.value)
            }

            ActionSource.X_BUTTON -> {
                PropertiesBuilder()
                    .add("in_app_message_id", inAppMessageId)
                    .add("in_app_message_key", inAppMessageKey)
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
            .add("in_app_message_key", inAppMessageKey)

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
