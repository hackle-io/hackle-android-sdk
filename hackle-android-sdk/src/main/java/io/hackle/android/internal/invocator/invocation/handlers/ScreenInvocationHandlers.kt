package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.checkParameterNotNull
import io.hackle.android.internal.invocator.className
import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.invocator.screenName
import io.hackle.sdk.common.Screen

// Screen

internal class SetCurrentScreenInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val screenName = checkParameterNotNull(request.parameters.screenName(), "screenName")
        val className = checkParameterNotNull(request.parameters.className(), "className")
        core.setCurrentScreen(Screen.builder(screenName, className).build())
        return InvocationResponse.success()
    }
}
