package io.hackle.android.ui.inappmessage.event

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.target.InAppMessageHiddenStorage
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.model.InAppMessage

internal interface InAppMessageActionHandler {
    fun supports(action: InAppMessage.Action): Boolean
    fun handle(layout: InAppMessageLayout, action: InAppMessage.Action)
}

internal class InAppMessageActionHandlerFactory(private val handlers: List<InAppMessageActionHandler>) {
    fun get(action: InAppMessage.Action): InAppMessageActionHandler? {
        return handlers.find { it.supports(action) }
    }
}

internal class InAppMessageCloseActionHandler : InAppMessageActionHandler {
    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType == InAppMessage.ActionType.CLOSE
    }

    override fun handle(layout: InAppMessageLayout, action: InAppMessage.Action) {
        layout.close()
    }
}

internal class InAppMessageLinkActionHandler(private val uriHandler: UriHandler) :
    InAppMessageActionHandler {

    private val log = Logger<InAppMessageLinkActionHandler>()

    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType == InAppMessage.ActionType.WEB_LINK
    }

    override fun handle(layout: InAppMessageLayout, action: InAppMessage.Action) {
        val activity = layout.activity
        if (activity == null) {
            log.warn { "InAppMessage activity is null, not handle action [${layout.context.inAppMessage.id}]" }
            return
        }

        val link = action.value
        if (link == null) {
            log.error { "InAppMessage action value is null, not handle action [${layout.context.inAppMessage.id}]" }
            return
        }
        uriHandler.handle(activity, link)
    }
}

internal class InAppMessageLinkAndCloseActionHandler(private val uriHandler: UriHandler) :
    InAppMessageActionHandler {

    private val log = Logger<InAppMessageLinkActionHandler>()

    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType == InAppMessage.ActionType.LINK_AND_CLOSE
    }

    override fun handle(layout: InAppMessageLayout, action: InAppMessage.Action) {
        val activity = layout.activity
        if (activity == null) {
            log.warn { "InAppMessage activity is null, not handle action [${layout.context.inAppMessage.id}]" }
            return
        }

        val link = action.value
        if (link == null) {
            log.error { "InAppMessage action value is null, not handle action [${layout.context.inAppMessage.id}]" }
            return
        }
        layout.close()
        uriHandler.handle(activity, link)
    }
}

internal class InAppMessageHideActionHandler(
    private val storage: InAppMessageHiddenStorage,
    private val clock: Clock
) : InAppMessageActionHandler {
    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType == InAppMessage.ActionType.HIDDEN
    }

    override fun handle(layout: InAppMessageLayout, action: InAppMessage.Action) {
        if (layout.context.decisionReason == DecisionReason.OVERRIDDEN) {
            layout.close()
            return
        }

        val expireAt = clock.currentMillis() + DEFAULT_HIDDEN_DURATION_MILLIS
        storage.put(layout.context.inAppMessage, expireAt)
        layout.close()
    }

    companion object {
        private const val DEFAULT_HIDDEN_DURATION_MILLIS = 1000 * 60 * 60 * 24 // 24H
    }
}


internal class UriHandler {

    fun handle(context: Context, link: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(link)
        context.startActivity(intent)
    }
}
