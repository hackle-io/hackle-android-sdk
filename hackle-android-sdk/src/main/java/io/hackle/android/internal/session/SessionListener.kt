package io.hackle.android.internal.session

internal interface SessionListener {

    fun onSessionStarted(session: Session, timestamp: Long)

    fun onSessionEnded(session: Session, timestamp: Long)
}
