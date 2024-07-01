package io.hackle.android.ui.inappmessage.layout.view

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.sdk.core.model.InAppMessage.DisplayType.BANNER
import io.hackle.sdk.core.model.InAppMessage.DisplayType.MODAL
import io.hackle.sdk.core.model.InAppMessage.LayoutType.*

internal class InAppMessageViewFactory {

    fun create(context: InAppMessagePresentationContext, activity: Activity): InAppMessageView {
        val view = when (context.message.layout.displayType) {
            BANNER -> createBannerView(context, activity)
            MODAL -> throw IllegalArgumentException("Unsupported InAppMessage display type [${context.inAppMessage.id}, ${context.message.layout.displayType}]")
        }
        view.setContext(context)
        return view
    }

    private fun createBannerView(context: InAppMessagePresentationContext, activity: Activity): InAppMessageView {
        return when (context.message.layout.layoutType) {
            IMAGE_ONLY, IMAGE -> InAppMessageBannerImageView.create(activity)
            IMAGE_TEXT, TEXT_ONLY -> InAppMessageBannerView.create(activity)
        }
    }
}
