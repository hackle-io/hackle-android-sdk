package io.hackle.android.ui.inappmessage.event

import io.hackle.sdk.core.model.InAppMessage

internal sealed class InAppMessageEvent {
    object Impression : InAppMessageEvent()
    object Close : InAppMessageEvent()
    class Action(
        val action: InAppMessage.Action,
        val area: InAppMessage.ActionArea,
        val text: String? = null
    ) : InAppMessageEvent()
}
