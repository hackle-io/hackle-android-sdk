package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.checkParameterNotNull
import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.invocator.optOut

// OptOutTracking

internal class SetOptOutTrackingInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val optOut = checkParameterNotNull(request.parameters.optOut(), "optOut")
        core.setOptOutTracking(optOut)
        return InvocationResponse.success()
    }
}
