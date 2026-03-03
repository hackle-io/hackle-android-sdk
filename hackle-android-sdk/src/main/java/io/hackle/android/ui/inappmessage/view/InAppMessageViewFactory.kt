package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.view.banner.InAppMessageBannerImageView
import io.hackle.android.ui.inappmessage.view.banner.InAppMessageBannerView
import io.hackle.android.ui.inappmessage.view.modal.InAppMessageModalView
import io.hackle.android.ui.inappmessage.view.sheet.InAppMessageBottomSheetView
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.LayoutType.*

internal class InAppMessageViewFactory {

    fun create(context: InAppMessagePresentationContext, activity: Activity): InAppMessageBaseView {
        val view = when (context.message.layout.displayType) {
            InAppMessage.DisplayType.NONE -> throw IllegalArgumentException("Unsupported in-app message display type [${context.inAppMessage.id}, ${context.message.layout.displayType}]")
            InAppMessage.DisplayType.MODAL -> InAppMessageModalView.create(activity)
            InAppMessage.DisplayType.BANNER -> createBannerView(context, activity)
            InAppMessage.DisplayType.BOTTOM_SHEET -> InAppMessageBottomSheetView.create(activity)
        }
        view.setPresentationContext(context)
        return view
    }

    private fun createBannerView(context: InAppMessagePresentationContext, activity: Activity): InAppMessageBaseView {
        return when (context.message.layout.layoutType) {
            NONE -> throw IllegalArgumentException("Unsupported in-app message layout type [${context.inAppMessage.id}, ${context.message.layout.layoutType}]")
            IMAGE_ONLY, IMAGE -> InAppMessageBannerImageView.create(activity)
            IMAGE_TEXT, TEXT_ONLY -> InAppMessageBannerView.create(activity)
        }
    }
}
