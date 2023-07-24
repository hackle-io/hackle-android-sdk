package io.hackle.android.ui.inappmessage.event

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.evaluation.target.InAppMessageHiddenStorage
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.model.InAppMessage

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
        return action.type == InAppMessage.ActionType.CLOSE
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        view.close()
    }
}

internal class InAppMessageLinkActionHandler(private val uriHandler: UriHandler) : InAppMessageActionHandler {

    private val log = Logger<InAppMessageLinkActionHandler>()

    override fun supports(action: InAppMessage.Action): Boolean {
        return action.type == InAppMessage.ActionType.WEB_LINK
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        val activity = view.activity
        if (activity == null) {
            log.warn { "InAppMessage activity is null, not handle action [${view.context.inAppMessage.id}]" }
            return
        }

        val link = action.value
        if (link == null) {
            log.error { "InAppMessage action value is null, not handle action [${view.context.inAppMessage.id}]" }
            return
        }
        uriHandler.handle(activity, link)
    }
}

internal class InAppMessageHideActionHandler(
    private val storage: InAppMessageHiddenStorage,
    private val clock: Clock
) : InAppMessageActionHandler {
    override fun supports(action: InAppMessage.Action): Boolean {
        return action.type == InAppMessage.ActionType.HIDDEN
    }

    override fun handle(view: InAppMessageView, action: InAppMessage.Action) {
        val expireAt = clock.currentMillis() + DEFAULT_HIDDEN_DURATION_MILLIS
        storage.put(view.context.inAppMessage, expireAt)
        view.close()
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
