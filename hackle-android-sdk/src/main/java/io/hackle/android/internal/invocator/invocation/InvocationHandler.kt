package io.hackle.android.internal.invocator.invocation

internal interface InvocationHandler<R> {
    fun invoke(request: InvocationRequest): InvocationResponse<R>
}
