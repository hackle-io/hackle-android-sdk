package io.hackle.android.ui.inappmessage.event

import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.HackleCore

internal class InAppMessageEventTracker(private val core: HackleCore) {

    fun track(context: InAppMessagePresentationContext, event: InAppMessageEvent, timestamp: Long) {
        val trackEvent = event.toTrackEvent(context)
        core.track(trackEvent, context.user, timestamp)
    }
}

internal fun InAppMessageEvent.toTrackEvent(context: InAppMessagePresentationContext): Event {
    return when (this) {
        is InAppMessageEvent.Impression ->
            Event.builder("\$in_app_impression")
                .properties(context.properties)
                .property("in_app_message_id", context.inAppMessage.id)
                .property("in_app_message_key", context.inAppMessage.key)
                .property("title_text", context.message.text?.title?.text)
                .property("body_text", context.message.text?.body?.text)
                .property("button_text", context.message.buttons.map { it.text })
                .property("image_url", context.message.images.map { it.imagePath })
                .build()

        is InAppMessageEvent.Close -> Event.builder("\$in_app_close")
            .properties(context.properties)
            .property("in_app_message_id", context.inAppMessage.id)
            .property("in_app_message_key", context.inAppMessage.key)
            .build()

        is InAppMessageEvent.Action -> Event.builder("\$in_app_action")
            .properties(context.properties)
            .property("in_app_message_id", context.inAppMessage.id)
            .property("in_app_message_key", context.inAppMessage.key)
            .property("action_type", action.type.name)
            .property("action_area", area.name)
            .property("action_value", action.value)
            .property("button_text", text)
            .build()
    }
}
