package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent.*
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.clock
import io.hackle.sdk.core.model.InAppMessage

/**
 * Represents event that has occurred in the [InAppMessageView].
 *
 * These are not tracking events (e.g., [io.hackle.sdk.common.Event]),
 * but view-level occurrences that trigger tracking and action handling.
 *
 * Each subtype describes a past fact:
 * - [Impression]: the view has been shown
 * - [Close]: the view has been closed (not a command to close)
 * - [Action]: the user has clicked a button, image, or link
 * - [ImageImpression]: an image has been shown
 */
internal sealed class InAppMessageViewEvent {

    abstract val type: Type
    abstract val timestamp: Long

    enum class Type {
        IMPRESSION,
        CLOSE,
        ACTION,
        IMAGE_IMPRESSION
    }

    class Impression(
        override val timestamp: Long,
    ) : InAppMessageViewEvent() {
        override val type: Type get() = Type.IMPRESSION
        override fun toString(): String {
            return "InAppMessageViewEvent.Impression(timestamp=$timestamp)"
        }
    }

    class Close(
        override val timestamp: Long,
    ) : InAppMessageViewEvent() {
        override val type: Type get() = Type.CLOSE
        override fun toString(): String {
            return "InAppMessageViewEvent.Close(timestamp=$timestamp)"
        }
    }

    class Action(
        override val timestamp: Long,
        val action: InAppMessage.Action,
        val area: InAppMessage.ActionArea?,
        val button: InAppMessage.Message.Button?,
        val image: InAppMessage.Message.Image?,
        val imageOrder: Int?,
        val elementId: String?,
    ) : InAppMessageViewEvent() {
        override val type: Type get() = Type.ACTION
        override fun toString(): String {
            return "InAppMessageViewEvent.Action(timestamp=$timestamp, action=$action)"
        }
    }

    class ImageImpression(
        override val timestamp: Long,
        val image: InAppMessage.Message.Image,
        val order: Int,
    ) : InAppMessageViewEvent() {
        override val type: Type get() = Type.IMAGE_IMPRESSION
        override fun toString(): String {
            return "InAppMessageViewEvent.ImageImpression(timestamp=$timestamp, image=$image, order=$order)"
        }
    }

    companion object {

        fun impression(view: InAppMessageView): Impression {
            return Impression(
                timestamp = view.clock.currentMillis()
            )
        }

        fun close(view: InAppMessageView): Close {
            return Close(
                timestamp = view.clock.currentMillis()
            )
        }

        fun imageImpression(view: InAppMessageView, image: InAppMessage.Message.Image, order: Int): ImageImpression {
            return ImageImpression(
                timestamp = view.clock.currentMillis(),
                image = image,
                order = order
            )
        }

        fun action(
            view: InAppMessageView,
            action: InAppMessage.Action,
            button: InAppMessage.Message.Button,
        ): Action {
            return Action(
                timestamp = view.clock.currentMillis(),
                action = action,
                area = InAppMessage.ActionArea.BUTTON,
                button = button,
                image = null,
                imageOrder = null,
                elementId = null,
            )
        }

        fun action(
            view: InAppMessageView,
            action: InAppMessage.Action,
            image: InAppMessage.Message.Image,
            order: Int?,
        ): Action {
            return Action(
                timestamp = view.clock.currentMillis(),
                action = action,
                area = InAppMessage.ActionArea.IMAGE,
                button = null,
                image = image,
                imageOrder = order,
                elementId = null,
            )
        }

        fun action(
            view: InAppMessageView,
            action: InAppMessage.Action,
            area: InAppMessage.ActionArea?,
            elementId: String? = null
        ): Action {
            return Action(
                timestamp = view.clock.currentMillis(),
                action = action,
                area = area,
                button = null,
                image = null,
                imageOrder = null,
                elementId = elementId,
            )
        }
    }
}
