package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.task.Task
import io.hackle.sdk.core.user.HackleUser

internal interface InAppMessageDeliverEvaluator {
    fun evaluate(request: InAppMessageDeliverRequest, user: HackleUser): Task<InAppMessageDeliverEvaluateResponse>
}
