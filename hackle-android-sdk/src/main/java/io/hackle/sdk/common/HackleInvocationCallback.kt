package io.hackle.sdk.common

/**
 * Callback that receives the result of an asynchronous invocation.
 */
fun interface HackleInvocationCallback {

    /**
     * Called when the invocation has been fully processed.
     *
     * @param response the result of the invocation
     */
    fun onResponse(response: String)
}
