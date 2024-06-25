package io.hackle.android.ui.inappmessage

import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.view.InAppMessageModalView
import io.hackle.android.ui.inappmessage.view.InAppMessageActivityController
import io.hackle.sdk.core.model.InAppMessage.DisplayType.BANNER
import io.hackle.sdk.core.model.InAppMessage.DisplayType.MODAL

internal class InAppMessageControllerFactory {
    fun create(context: InAppMessagePresentationContext, ui: InAppMessageUi): InAppMessageController {
        return when (context.message.layout.displayType) {
            MODAL -> InAppMessageActivityController.create<InAppMessageModalView>(context, ui)
            BANNER -> TODO()
        }
    }
}
