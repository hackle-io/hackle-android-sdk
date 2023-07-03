package io.hackle.android.internal.inappmessage

import io.hackle.sdk.core.model.InAppMessage

data class InAppMessageRenderSource(
    val inAppMessage: InAppMessage,
    val message: InAppMessage.MessageContext.Message
)