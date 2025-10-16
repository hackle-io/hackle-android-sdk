package io.hackle.android.internal.session

import io.hackle.android.internal.user.UserDecorator
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType

internal class SessionUserDecorator(
    private val sessionManager: SessionManager,
) : UserDecorator {
    override fun decorate(user: HackleUser): HackleUser {
        if (user.sessionId != null) {
            return user
        }

        val session = sessionManager.currentSession ?: return user

        return user.toBuilder()
            .identifier(IdentifierType.SESSION, session.id, overwrite = false)
            .build()
    }
}
