package io.hackle.android.internal.inappmessage.presentation

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser

internal data class InAppMessagePresentationContext(
    val inAppMessage: InAppMessage,
    val message: InAppMessage.Message,
    val user: HackleUser,
    val properties: Map<String, Any>,
    val triggerEventId: String,
    var decisionReason: DecisionReason
)
