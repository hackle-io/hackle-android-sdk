package io.hackle.android.ui.inappmessage.event.action

import android.net.Uri
import io.hackle.android.HackleUrlChannel
import io.hackle.android.ui.core.HackleUrlDispatcher
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.match.InAppMessageHiddenStorage
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

internal class InAppMessageLinkActionHandler(
    private val urlDispatcher: HackleUrlDispatcher,
) : InAppMessageActionHandler {

    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType.shouldLink && !action.actionType.shouldClose
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        urlDispatcher.openInAppMessageLink(view, action, close = false)
    }
}

internal class InAppMessageLinkAndCloseActionHandler(
    private val urlDispatcher: HackleUrlDispatcher,
) : InAppMessageActionHandler {

    override fun supports(action: InAppMessage.Action): Boolean {
        return action.actionType.shouldLink && action.actionType.shouldClose
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        urlDispatcher.openInAppMessageLink(view, action, close = true)
    }
}

private val linkActionLog = Logger<InAppMessageLinkActionHandler>()

/**
 * Opens the link of an in-app message [action] from the activity of [view].
 * Does nothing when the view has no activity or the action has no link.
 * When [close] is true, the view is closed before the link is opened.
 */
private fun HackleUrlDispatcher.openInAppMessageLink(
    view: InAppMessageView,
    action: InAppMessage.Action,
    close: Boolean,
) {
    val activity = view.activity
    if (activity == null) {
        linkActionLog.warn { "InAppMessage activity is null, not handle action [${view.inAppMessage.id}]" }
        return
    }

    val link = action.value
    if (link == null) {
        linkActionLog.error { "InAppMessage action value is null, not handle action [${view.inAppMessage.id}]" }
        return
    }
    if (close) {
        view.close()
    }
    open(activity, Uri.parse(link), HackleUrlChannel.IN_APP_MESSAGE)
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
