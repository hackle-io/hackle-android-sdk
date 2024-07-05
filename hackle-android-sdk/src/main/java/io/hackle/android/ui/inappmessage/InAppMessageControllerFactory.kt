package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.layout.activity.InAppMessageActivityController
import io.hackle.android.ui.inappmessage.layout.activity.InAppMessageModalActivity
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageViewController
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageViewFactory
import io.hackle.sdk.core.model.InAppMessage.DisplayType.*

internal class InAppMessageControllerFactory(
    private val viewFactory: InAppMessageViewFactory
) {
    fun create(
        context: InAppMessagePresentationContext,
        ui: InAppMessageUi,
        activity: Activity
    ): InAppMessageController? {
        return when (context.message.layout.displayType) {
            NONE -> null
            MODAL -> InAppMessageActivityController.create<InAppMessageModalActivity>(context, ui)
            BANNER -> createViewController(context, ui, activity)
        }
    }

    private fun createViewController(
        context: InAppMessagePresentationContext,
        ui: InAppMessageUi,
        activity: Activity
    ): InAppMessageViewController {
        val view = viewFactory.create(context, activity)
        val controller = InAppMessageViewController(view, context, ui)
        view.setController(controller)
        view.layout()
        return controller
    }
}
