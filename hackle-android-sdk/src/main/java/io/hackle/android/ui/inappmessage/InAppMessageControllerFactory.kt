package io.hackle.android.ui.inappmessage

import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.layout.activity.InAppMessageActivityController
import io.hackle.android.ui.inappmessage.layout.activity.InAppMessageModalActivity
import io.hackle.sdk.core.model.InAppMessage.DisplayType.BANNER
import io.hackle.sdk.core.model.InAppMessage.DisplayType.MODAL

internal class InAppMessageControllerFactory {
    fun create(context: InAppMessagePresentationContext, ui: InAppMessageUi): InAppMessageController {
        return when (context.message.layout.displayType) {
            MODAL -> InAppMessageActivityController.create<InAppMessageModalActivity>(context, ui)
            BANNER -> TODO()
        }
    }
}
