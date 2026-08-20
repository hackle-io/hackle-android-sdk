package io.hackle.sdk.common

/**
 * Interface for invoking Hackle operations.
 */
interface HackleInvocator {
    /**
     * Checks if the given string is invocable.
     *
     * @param string the string to check
     * @return true if the string is invocable, false otherwise
     */
    fun isInvocableString(string: String): Boolean
    
    /**
     * Invokes the operation specified by the given string.
     *
     * @param string the string specifying the operation to invoke
     * @return the result of the invocation
     */
    fun invoke(string: String): String

    /**
     * Invokes the operation specified by the given string and calls the callback when the operation is complete.
     *
     * For user mutation commands (`setUser`, `setUserId`, `setDeviceId`, `resetUser`, `setUserProperty`,
     * `updateUserProperties`) the callback is called after the user context synchronization is complete.
     * For every other command the callback is called immediately.
     *
     * The calling thread of the callback depends on the command: for immediate commands it is invoked
     * synchronously on the calling thread, while for user mutation commands it is invoked later on
     * whichever background thread completes the underlying operation. Callers must not assume either
     * thread, and must dispatch to the main thread themselves before touching UI from the callback.
     *
     * @param string the string specifying the operation to invoke
     * @param callback called with the result of the invocation
     */
    fun invokeAsync(string: String, callback: HackleInvocationCallback)
}
