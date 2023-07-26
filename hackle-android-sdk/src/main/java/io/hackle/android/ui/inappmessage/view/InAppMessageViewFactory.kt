package io.hackle.android.ui.inappmessage.view

import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageUi

internal class InAppMessageViewFactory {

    fun create(context: InAppMessagePresentationContext, ui: InAppMessageUi): InAppMessageView {
        return ActivityInAppMessageView(context, ui)
    }
}
