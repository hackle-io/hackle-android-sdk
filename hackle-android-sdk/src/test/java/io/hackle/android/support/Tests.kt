package io.hackle.android.support

import org.opentest4j.AssertionFailedError


internal inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (T::class.java.isInstance(e)) {
            return e as T
        } else {
            val message =
                "Unexpected exception type thrown ===> expected: ${T::class.java.canonicalName} but was: ${e::class.java.canonicalName}"
            throw AssertionFailedError(message, e)
        }
    }

    val message = "Expected ${T::class.java.canonicalName} to be thrown, but nothing was thrown."
    throw AssertionFailedError(message)
}