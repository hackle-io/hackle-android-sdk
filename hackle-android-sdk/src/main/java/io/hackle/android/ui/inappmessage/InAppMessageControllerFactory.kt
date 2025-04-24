package io.hackle.android.ui.inappmessage

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageViewController
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageViewFactory
import io.hackle.sdk.core.model.InAppMessage
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
            MODAL, BANNER, BOTTOM_SHEET -> createViewController(context, ui, activity)
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

        if(controller.context.message.layout.displayType == BANNER || controller.context.message.layout.displayType == BOTTOM_SHEET) {
            setOnApplyWindowInsetsListener(view, controller)
        }

        view.configure()
        return controller
    }

    // add margin when enableEdgeToEdge
    // ref. https://developer.android.com/develop/ui/views/layout/edge-to-edge#system-bars-insets
    @SuppressLint("RestrictedApi")
    private fun setOnApplyWindowInsetsListener(view: View, controller: InAppMessageController) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val inAppMessageAlignment = controller.context.message.layout.alignment?.vertical

            if(inAppMessageAlignment == InAppMessage.Message.Alignment.Vertical.TOP) {
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin += windowInsets.systemWindowInsetTop
                }
            } else if(inAppMessageAlignment == InAppMessage.Message.Alignment.Vertical.BOTTOM) {
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin += windowInsets.systemWindowInsetBottom
                }
            }
            // 마진 적용 후 리스너 해제
            ViewCompat.setOnApplyWindowInsetsListener(v, null)

            WindowInsetsCompat.CONSUMED
        }
    }
}
