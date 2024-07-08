package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageViewController
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageViewFactory

internal class InAppMessageControllerFactory(
    private val viewFactory: InAppMessageViewFactory
) {
    fun create(
        context: InAppMessagePresentationContext,
        ui: InAppMessageUi,
        activity: Activity
    ): InAppMessageController {
        val view = viewFactory.create(context, activity)
        val controller = InAppMessageViewController(view, context, ui)
        view.setController(controller)
        view.configure()
        return controller
    }
}
