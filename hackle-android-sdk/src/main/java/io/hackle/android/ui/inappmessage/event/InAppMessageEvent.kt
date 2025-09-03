package io.hackle.android.ui.inappmessage.event

import io.hackle.sdk.core.model.InAppMessage

internal sealed class InAppMessageEvent {
    object Impression : InAppMessageEvent() {
        override fun toString(): String = "InAppMessageEvent.Impression"
    }

    object Close : InAppMessageEvent() {
        override fun toString(): String = "InAppMessageEvent.Close"
    }

    class Action(
        val action: InAppMessage.Action,
        val area: InAppMessage.ActionArea,
        val button: InAppMessage.Message.Button?,
        val image: InAppMessage.Message.Image?,
        val imageOrder: Int?,
    ) : InAppMessageEvent() {
        override fun toString(): String {
            return "InAppMessageEvent.Action(action=$action, area=$area)"
        }
    }

    class ImageImpression(
        val image: InAppMessage.Message.Image,
        val order: Int,
    ) : InAppMessageEvent() {
        override fun toString(): String {
            return "InAppMessageEvent.ImageImpression(order=$order)"
        }
    }

    companion object {
        fun buttonAction(action: InAppMessage.Action, button: InAppMessage.Message.Button): Action {
            return Action(
                action = action,
                area = InAppMessage.ActionArea.BUTTON,
                button = button,
                image = null,
                imageOrder = null,
            )
        }

        fun imageAction(
            action: InAppMessage.Action,
            image: InAppMessage.Message.Image,
            order: Int?,
        ): Action {
            return Action(
                action = action,
                area = InAppMessage.ActionArea.IMAGE,
                button = null,
                image = image,
                imageOrder = order,
            )
        }

        fun closeButtonAction(action: InAppMessage.Action): Action {
            return Action(
                action = action,
                area = InAppMessage.ActionArea.X_BUTTON,
                button = null,
                image = null,
                imageOrder = null,
            )
        }

        fun messageAction(action: InAppMessage.Action): Action {
            return Action(
                action = action,
                area = InAppMessage.ActionArea.MESSAGE,
                button = null,
                image = null,
                imageOrder = null,
            )
        }
    }
}
