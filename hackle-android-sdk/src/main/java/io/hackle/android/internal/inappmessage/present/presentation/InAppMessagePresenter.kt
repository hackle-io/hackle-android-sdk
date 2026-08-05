package io.hackle.android.internal.inappmessage.present.presentation

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse
import java.util.concurrent.CompletableFuture

internal interface InAppMessagePresenter {
    fun present(context: InAppMessagePresentationContext): CompletableFuture<InAppMessagePresentResponse>
}
