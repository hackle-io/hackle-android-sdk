package io.hackle.android.ui.inappmessage.layout.view

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.layout.view.banner.InAppMessageBannerImageView
import io.hackle.android.ui.inappmessage.layout.view.banner.InAppMessageBannerView
import io.hackle.android.ui.inappmessage.layout.view.modal.InAppMessageModalView
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.LayoutType.*

internal class InAppMessageViewFactory {

    fun create(context: InAppMessagePresentationContext, activity: Activity): InAppMessageView {
        val view = when (context.message.layout.displayType) {
            InAppMessage.DisplayType.NONE -> throw IllegalArgumentException("Unsupported in-app message display type [${context.inAppMessage.id}, ${context.message.layout.displayType}]")
            InAppMessage.DisplayType.MODAL -> InAppMessageModalView.create(activity)
            InAppMessage.DisplayType.BANNER -> createBannerView(context, activity)
        }
        view.setContext(context)
        return view
    }

    private fun createBannerView(context: InAppMessagePresentationContext, activity: Activity): InAppMessageView {
        return when (context.message.layout.layoutType) {
            NONE -> throw IllegalArgumentException("Unsupported in-app message layout type [${context.inAppMessage.id}, ${context.message.layout.layoutType}]")
            IMAGE_ONLY, IMAGE -> InAppMessageBannerImageView.create(activity)
            IMAGE_TEXT, TEXT_ONLY -> InAppMessageBannerView.create(activity)
        }
    }
}
