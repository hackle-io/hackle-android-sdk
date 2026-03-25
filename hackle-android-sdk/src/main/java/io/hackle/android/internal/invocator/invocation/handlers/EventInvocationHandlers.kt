package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.invocator.checkParameterNotNull
import io.hackle.android.internal.invocator.event
import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.invocator.user

// Track

internal class TrackInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val event = checkParameterNotNull(request.parameters.event(), "event")
        val context = HackleAppContext.create(request.browserProperties)
        core.track(event, request.parameters.user(), context)
        return InvocationResponse.success()
    }
}
