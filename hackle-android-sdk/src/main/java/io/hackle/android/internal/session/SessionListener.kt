package io.hackle.android.internal.session

import io.hackle.android.internal.core.listener.ApplicationListener
import io.hackle.sdk.common.User

internal interface SessionListener : ApplicationListener {
    fun onSessionStarted(session: Session, user: User, timestamp: Long)
    fun onSessionEnded(session: Session, user: User, timestamp: Long)
}
