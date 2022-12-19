package io.hackle.android.internal.session

import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppInitializeListener
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.android.internal.user.UserListener
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.user.HackleUser
import java.util.concurrent.Executor

internal class SessionManager(
    private val eventExecutor: Executor,
    private val keyValueRepository: KeyValueRepository,
    private val sessionTimeoutMillis: Long,
) : AppInitializeListener, AppStateChangeListener, UserListener {

    private val sessionListeners = mutableListOf<SessionListener>()

    val requiredSession: Session get() = currentSession ?: Session.UNKNOWN

    var currentSession: Session? = null
        private set

    var lastEventTime: Long? = null
        private set

    fun addListener(listener: SessionListener) {
        sessionListeners.add(listener)
        log.debug { "SessionListener added [${listener::class.java.simpleName}]" }
    }

    fun startNewSession(timestamp: Long): Session {
        endSession()
        return newSession(timestamp)
    }

    fun startNewSessionIfNeeded(timestamp: Long): Session {

        val lastEventTime = lastEventTime ?: return startNewSession(timestamp)

        val currentSession = currentSession
        return if (currentSession != null && timestamp - lastEventTime < sessionTimeoutMillis) {
            updateLastEventTime(timestamp)
            currentSession
        } else {
            startNewSession(timestamp)
        }
    }

    fun updateLastEventTime(timestamp: Long) {
        lastEventTime = timestamp
        keyValueRepository.putLong(LAST_EVENT_TIME_KEY, timestamp)
        log.debug { "LastEventTime updated [$timestamp]" }
    }

    private fun endSession() {
        val oldSession = currentSession
        val lastEventTime = lastEventTime

        if (oldSession != null && lastEventTime != null) {
            for (listener in sessionListeners) {
                listener.onSessionEnded(oldSession, lastEventTime)
            }
            log.debug { "Session ended [${oldSession.id}]" }
        }
    }

    private fun newSession(timestamp: Long): Session {
        val newSession = Session.create(timestamp)
        currentSession = newSession
        saveSession(newSession)

        updateLastEventTime(timestamp)

        for (listener in sessionListeners) {
            listener.onSessionStarted(newSession, timestamp)
        }

        log.debug { "Session started [${newSession.id}]" }
        return newSession
    }

    private fun saveSession(session: Session) {
        keyValueRepository.putString(SESSION_ID_KEY, session.id)
        log.debug { "Session saved [${session.id}]" }
    }

    private fun loadSession() {
        val sessionId = keyValueRepository.getString(SESSION_ID_KEY)
        currentSession = sessionId?.let { Session(id = it) }
        log.debug { "Session loaded [$sessionId]" }
    }

    private fun loadLastEventTime() {
        val lastEventTime = keyValueRepository.getLong(LAST_EVENT_TIME_KEY, -1)
        if (lastEventTime > 0) {
            this.lastEventTime = lastEventTime
        }
        log.debug { "LastEventTime loaded [${this.lastEventTime}]" }
    }

    override fun onInitialized() {
        eventExecutor.execute {
            log.debug { "SessionManager initialize start." }
            loadSession()
            loadLastEventTime()
            log.debug { "SessionManager initialize end." }
        }
    }

    override fun onChanged(state: AppState, timestamp: Long) {
        return when (state) {
            FOREGROUND -> eventExecutor.execute { startNewSessionIfNeeded(timestamp) }
            BACKGROUND -> eventExecutor.execute {
                updateLastEventTime(timestamp)
                currentSession?.let { saveSession(it) }
            }
        }
    }

    override fun onUserUpdated(user: HackleUser, timestamp: Long) {
        startNewSession(timestamp)
    }

    companion object {
        private val log = Logger<SessionManager>()
        private const val SESSION_ID_KEY = "session_id"
        private const val LAST_EVENT_TIME_KEY = "last_event_time"
    }
}
