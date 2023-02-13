package io.hackle.android.internal.session

import io.hackle.sdk.common.User

internal interface SessionListener {

    fun onSessionStarted(session: Session, user: User, timestamp: Long)

    fun onSessionEnded(session: Session, user: User, timestamp: Long)
}
