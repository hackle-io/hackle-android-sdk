package io.hackle.android.ui.inappmessage.event

import io.hackle.android.internal.event.InternalEvent
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.sdk.core.HackleCore

internal class InAppMessageEventTracker(private val core: HackleCore) {

    fun track(context: InAppMessagePresentationContext, event: InAppMessageEvent, timestamp: Long) {
        val trackEvent = event.toTrackEvent(context)
        core.track(trackEvent, context.user, timestamp)
    }
}

internal fun InAppMessageEvent.toTrackEvent(context: InAppMessagePresentationContext): InternalEvent {
    return when (this) {
        is InAppMessageEvent.Impression ->
            InternalEvent.builder("\$in_app_impression")
                .properties(context)
                .property("title_text", context.message.text?.title?.text)
                .property("body_text", context.message.text?.body?.text)
                .property("button_text", context.message.buttons.map { it.text })
                .property("image_url", context.message.images.map { it.imagePath })
                .build()

        is InAppMessageEvent.ImageImpression ->
            InternalEvent.builder("\$in_app_image_impression")
                .properties(context)
                .property("image_url", image.imagePath)
                .property("image_order", order)
                .build()

        is InAppMessageEvent.Close -> InternalEvent.builder("\$in_app_close")
            .properties(context)
            .build()

        is InAppMessageEvent.Action -> InternalEvent.builder("\$in_app_action")
            .properties(context)
            .property("action_type", action.actionType.name)
            .property("action_area", area.name)
            .property("action_value", action.value)
            .property("button_text", button?.text)
            .property("image_url", image?.imagePath)
            .property("image_order", imageOrder)
            .build()
    }
}

private fun InternalEvent.Builder.properties(context: InAppMessagePresentationContext) = apply {
    properties(context.properties)
    property("in_app_message_id", context.inAppMessage.id)
    property("in_app_message_key", context.inAppMessage.key)
    property("in_app_message_display_type", context.message.layout.displayType.name)
    internalProperty("\$trigger_event_insert_id", context.triggerEventId)
}
