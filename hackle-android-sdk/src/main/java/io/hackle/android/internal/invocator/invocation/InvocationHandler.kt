package io.hackle.android.internal.invocator.invocation

internal interface InvocationHandler<out R> {
    fun invoke(request: InvocationRequest): InvocationResponse<R>
}
