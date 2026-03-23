package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import android.os.Build
import androidx.webkit.WebViewAssetLoader
import io.hackle.android.Hackle
import io.hackle.android.app
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.view.banner.InAppMessageBannerImageView
import io.hackle.android.ui.inappmessage.view.banner.InAppMessageBannerView
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlBridgeUserScript
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlContentResolverFactory
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlView
import io.hackle.android.ui.inappmessage.view.modal.InAppMessageModalView
import io.hackle.android.ui.inappmessage.view.sheet.InAppMessageBottomSheetView
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.LayoutType.*

internal class InAppMessageViewFactory(
    private val htmlContentResolverFactory: InAppMessageHtmlContentResolverFactory,
    private val assetLoader: WebViewAssetLoader,
) {

    fun create(context: InAppMessagePresentationContext, activity: Activity): InAppMessageBaseView {
        val view = when (context.message.layout.displayType) {
            InAppMessage.DisplayType.NONE -> throw IllegalArgumentException("Unsupported in-app message display type [${context.inAppMessage.id}, ${context.message.layout.displayType}]")
            InAppMessage.DisplayType.MODAL -> InAppMessageModalView.create(activity)
            InAppMessage.DisplayType.BANNER -> createBannerView(context, activity)
            InAppMessage.DisplayType.BOTTOM_SHEET -> InAppMessageBottomSheetView.create(activity)
            InAppMessage.DisplayType.HTML -> createHtmlView(activity)
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


    private fun createHtmlView(activity: Activity): InAppMessageHtmlView {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val bridgeScript = InAppMessageHtmlBridgeUserScript.create(Hackle.app.config)
            return InAppMessageHtmlView.create(activity, bridgeScript, assetLoader, htmlContentResolverFactory)
        }

        throw IllegalStateException("InAppMessageHtmlView required API 17+ (current: ${Build.VERSION.SDK_INT})")
    }
}
