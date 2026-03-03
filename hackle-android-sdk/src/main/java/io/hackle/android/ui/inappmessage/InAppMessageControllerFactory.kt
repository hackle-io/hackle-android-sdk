package io.hackle.android.ui.inappmessage

import android.app.Activity
import androidx.core.view.ViewCompat
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.view.BaseInAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageViewController
import io.hackle.android.ui.inappmessage.view.InAppMessageViewFactory
import io.hackle.sdk.core.model.InAppMessage.DisplayType.*

internal class InAppMessageControllerFactory(
    private val viewFactory: InAppMessageViewFactory,
) {
    fun create(
        context: InAppMessagePresentationContext,
        ui: InAppMessageUi,
        activity: Activity,
    ): InAppMessageController? {
        return when (context.message.layout.displayType) {
            NONE -> null
            MODAL, BANNER, BOTTOM_SHEET -> createViewController(context, ui, activity)
        }
    }

    private fun createViewController(
        context: InAppMessagePresentationContext,
        ui: InAppMessageUi,
        activity: Activity,
    ): InAppMessageViewController {
        val view = viewFactory.create(context, activity)
        val controller = InAppMessageViewController(view, ui)
        view.setController(controller)
        setOnApplyWindowInsetsListener(view)

        view.configure()
        return controller
    }

    // add margin when enableEdgeToEdge
    // ref. https://developer.android.com/develop/ui/views/layout/edge-to-edge#system-bars-insets
    private fun setOnApplyWindowInsetsListener(view: BaseInAppMessageView) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            (v as? BaseInAppMessageView)?.onApplyWindowInsets(windowInsets)
            windowInsets
        }
    }
}
