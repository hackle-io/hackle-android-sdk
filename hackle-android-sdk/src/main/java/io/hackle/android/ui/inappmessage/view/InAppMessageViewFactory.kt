package io.hackle.android.ui.inappmessage.view

import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.sdk.core.model.InAppMessage.DisplayType.BANNER
import io.hackle.sdk.core.model.InAppMessage.DisplayType.MODAL

internal class InAppMessageViewFactory {

    fun create(context: InAppMessagePresentationContext, ui: InAppMessageUi): InAppMessageView {
        return when (context.message.layout.displayType) {
            MODAL -> InAppMessageModalView(context, ui)
            BANNER -> throw IllegalArgumentException(context.message.layout.displayType.name)
        }
    }
}
