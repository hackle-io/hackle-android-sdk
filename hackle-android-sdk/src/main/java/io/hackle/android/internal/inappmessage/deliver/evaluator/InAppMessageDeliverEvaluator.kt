package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.sdk.core.user.HackleUser
import java.util.concurrent.CompletableFuture

internal interface InAppMessageDeliverEvaluator {
    fun evaluate(
        request: InAppMessageDeliverRequest,
        user: HackleUser
    ): CompletableFuture<InAppMessageDeliverEvaluateResponse>
}
