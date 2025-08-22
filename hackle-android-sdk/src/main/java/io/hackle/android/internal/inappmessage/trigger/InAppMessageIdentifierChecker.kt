package io.hackle.android.internal.inappmessage.trigger

import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.user.IdentifierType

/**
 * Identifier change checker for InAppMessage
 */
class InAppMessageIdentifierChecker {
    fun isIdentifierChanged(old: Identifiers, new: Identifiers): Boolean {
        val oldUserId = old[IdentifierType.USER]
        val newUserId = new[IdentifierType.USER]
        if (oldUserId != null && newUserId != null) {
            return oldUserId != newUserId
        }

        val oldDeviceId = old[IdentifierType.DEVICE]
        val newDeviceId = new[IdentifierType.DEVICE]
        if (oldDeviceId != null && newDeviceId != null) {
            return oldDeviceId != newDeviceId
        }

        return false
    }
}
