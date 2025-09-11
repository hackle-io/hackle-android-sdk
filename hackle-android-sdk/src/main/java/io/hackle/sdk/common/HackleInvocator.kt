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
}
