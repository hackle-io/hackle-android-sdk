package io.hackle.android.ui.inappmessage.event.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.target.InAppMessageHiddenStorage
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.Action.Companion.DEFAULT_HIDE_DURATION_MILLIS
import io.hackle.sdk.core.model.InAppMessage.ActionType

internal interface InAppMessageActionHandler {
    fun supports(action: InAppMessage.Action): Boolean
    fun handle(view: InAppMessageView, action: InAppMessage.Action)
}

internal class InAppMessageActionHandlerFactory(private val handlers: List<InAppMessageActionHandler>) {
    fun get(action: InAppMessage.Action): InAppMessageActionHandler? {
        return handlers.find { it.supports(action) }
    }
}

internal class InAppMessageCloseActionHandler : InAppMessageActionHandler {
    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType == ActionType.CLOSE
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        view.close()
    }
}

internal class InAppMessageLinkActionHandler(private val uriHandler: UriHandler) :
    InAppMessageActionHandler {

    private val log = Logger<InAppMessageLinkActionHandler>()

    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType.shouldLink && !action.actionType.shouldClose
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        val activity = view.activity
        if (activity == null) {
            log.warn { "InAppMessage activity is null, not handle action [${view.inAppMessage.id}]" }
            return
        }

        val link = action.value
        if (link == null) {
            log.error { "InAppMessage action value is null, not handle action [${view.inAppMessage.id}]" }
            return
        }
        uriHandler.handle(activity, link)
    }
}

internal class InAppMessageLinkAndCloseActionHandler(private val uriHandler: UriHandler) :
    InAppMessageActionHandler {

    private val log = Logger<InAppMessageLinkActionHandler>()

    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType.shouldLink && action.actionType.shouldClose
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        val activity = view.activity
        if (activity == null) {
            log.warn { "InAppMessage activity is null, not handle action [${view.inAppMessage.id}]" }
            return
        }

        val link = action.value
        if (link == null) {
            log.error { "InAppMessage action value is null, not handle action [${view.inAppMessage.id}]" }
            return
        }
        view.close()
        uriHandler.handle(activity, link)
    }
}

internal class InAppMessageHideActionHandler(
    private val storage: InAppMessageHiddenStorage,
    private val clock: Clock,
) : InAppMessageActionHandler {
    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType == ActionType.HIDDEN
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        if (view.presentationContext.decisionReason == DecisionReason.OVERRIDDEN) {
            view.close()
            return
        }

        val durationMillis = action.hideDurationMillis ?: DEFAULT_HIDE_DURATION_MILLIS
        val expireAt = clock.currentMillis() + durationMillis
        storage.put(view.inAppMessage, expireAt)
        view.close()
    }
}


internal class UriHandler {

    private val log = Logger<UriHandler>()

    fun handle(context: Context, link: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(link)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            log.error { "Failed to handle URI: $link\n$e" }
        }
    }
}
