package io.hackle.android.ui.inappmessage

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.view.ViewCompat
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

        // add margin when enableEdgeToEdge
        // ref. https://developer.android.com/develop/ui/views/layout/edge-to-edge#system-bars-insets
        if(controller.context.message.layout.displayType == BANNER || controller.context.message.layout.displayType == BOTTOM_SHEET) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val inAppMessageAlignment = controller.context.message.layout.alignment?.vertical

                if(inAppMessageAlignment == InAppMessage.Message.Alignment.Vertical.TOP) {
                    v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        val statusBarHeight = getStatusBarHeight(activity)
                        topMargin += statusBarHeight
                    }
                } else if(inAppMessageAlignment == InAppMessage.Message.Alignment.Vertical.BOTTOM) {
                    v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        val navigationBarHeight = getNavigationBarHeight(activity)
                        bottomMargin += navigationBarHeight
                    }
                }



                windowInsets
            }
        }

        view.configure()
        return controller
    }

    @SuppressLint("InternalInsetResource")
    private fun getNavigationBarHeight(activity: Activity): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = activity.window.decorView.rootWindowInsets
            return insets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
        } else {
            val resourceId =
                activity.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                activity.resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }
    }

    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = activity.window.decorView.rootWindowInsets
            insets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
        } else {
            val resourceId = activity.resources.getIdentifier(
                "status_bar_height", "dimen", "android"
            )
            if (resourceId > 0) {
                activity.resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }
    }
}
