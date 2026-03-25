package io.hackle.android.ui.inappmessage.event

import io.hackle.android.support.InAppMessages
import io.hackle.sdk.core.model.InAppMessage

internal object InAppMessageViewEvents {

    fun impression(
        timestamp: Long = System.currentTimeMillis()
    ): InAppMessageViewEvent.Impression {
        return InAppMessageViewEvent.Impression(
            timestamp = timestamp
        )
    }

    fun close(
        timestamp: Long = System.currentTimeMillis()
    ): InAppMessageViewEvent.Close {
        return InAppMessageViewEvent.Close(
            timestamp = timestamp
        )
    }

    fun action(
        timestamp: Long = System.currentTimeMillis(),
        action: InAppMessage.Action = InAppMessages.action(type = InAppMessage.ActionType.LINK_AND_CLOSE),
        area: InAppMessage.ActionArea? = null,
        button: InAppMessage.Message.Button? = null,
        image: InAppMessage.Message.Image? = null,
        imageOrder: Int? = null,
        elementId: String? = null
    ): InAppMessageViewEvent.Action {
        return InAppMessageViewEvent.Action(
            timestamp = timestamp,
            action = action,
            area = area,
            button = button,
            image = image,
            imageOrder = imageOrder,
            elementId = elementId
        )
    }
}
