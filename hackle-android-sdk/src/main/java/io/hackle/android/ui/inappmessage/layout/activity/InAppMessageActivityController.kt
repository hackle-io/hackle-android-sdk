package io.hackle.android.ui.inappmessage.layout.activity

import android.app.Activity
import android.content.Intent
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.handle
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout.State
import io.hackle.sdk.core.internal.log.Logger
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

internal class InAppMessageActivityController private constructor(
    override val context: InAppMessagePresentationContext,
    override val ui: InAppMessageUi,
    private val activityClass: Class<out InAppMessageActivity>,
) : InAppMessageController {

    private var messageActivity: WeakReference<InAppMessageActivity>? = null
    override val layout: InAppMessageLayout get() = checkNotNull(messageActivity?.get()) { "InAppMessageActivity not opened [${activityClass.simpleName}]" }

    private val _state = AtomicReference(State.CLOSED)
    val state: State get() = _state.get()

    override fun open(activity: Activity) {
        if (!_state.compareAndSet(State.CLOSED, State.OPENED)) {
            log.debug { "InAppMessage is already open (key=${context.inAppMessage.key})" }
            return
        }

        val intent = Intent(activity, activityClass)
        intent.putExtra(IN_APP_MESSAGE_ID, context.inAppMessage.id)
        activity.startActivity(intent)
    }

    fun onOpen(messageActivity: InAppMessageActivity) {
        this.messageActivity = WeakReference(messageActivity)
        handle(InAppMessageEvent.Impression)
    }

    override fun close(withAnimation: Boolean) {
        if (!_state.compareAndSet(State.OPENED, State.CLOSED)) {
            log.debug { "InAppMessage is already close (key=${context.inAppMessage.key})" }
            return
        }

        handle(InAppMessageEvent.Close)
        messageActivity?.get()?.activity?.finish()
        ui.closeCurrent()
    }

    companion object {
        private val log = Logger<InAppMessageActivityController>()
        const val IN_APP_MESSAGE_ID = "hackleInAppMessageId"
        inline fun <reified ACTIVITY : InAppMessageActivity> create(
            context: InAppMessagePresentationContext,
            ui: InAppMessageUi,
        ): InAppMessageActivityController {
            return InAppMessageActivityController(context, ui, ACTIVITY::class.java)
        }
    }
}
