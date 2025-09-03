package io.hackle.android.internal.inappmessage.reset

import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayManager
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageIdentifierChecker
import io.hackle.android.internal.user.resolvedIdentifiers
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageResetProcessor(
    private val identifierChecker: InAppMessageIdentifierChecker,
    private val delayManager: InAppMessageDelayManager,
) {
    fun process(oldUser: User, newUser: User) {
        try {
            val isIdentifierChanged =
                identifierChecker.isIdentifierChanged(oldUser.resolvedIdentifiers, newUser.resolvedIdentifiers)
            if (isIdentifierChanged) {
                reset()
            }
        } catch (e: Exception) {
            log.error { "Failed to reset InAppMessage: $e" }
        }
    }

    private fun reset() {
        val delayed = delayManager.cancelAll()
        log.debug { "InAppMessage Reset: cancelled=${delayed.count()}" }
    }

    companion object {
        private val log = Logger<InAppMessageResetProcessor>()
    }
}
