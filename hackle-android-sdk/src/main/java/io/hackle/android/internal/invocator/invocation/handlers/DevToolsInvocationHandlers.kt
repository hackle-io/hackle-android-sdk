package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse

// DevTools

internal class ShowUserExplorerInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        core.showUserExplorer()
        return InvocationResponse.success()
    }
}

internal class HideUserExplorerInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        core.hideUserExplorer()
        return InvocationResponse.success()
    }
}
