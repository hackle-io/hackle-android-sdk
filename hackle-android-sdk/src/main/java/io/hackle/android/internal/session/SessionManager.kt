package io.hackle.android.internal.session

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CopyOnWriteArrayList

internal class SessionManager(
    private val userManager: UserManager,
    private val keyValueRepository: KeyValueRepository,
    private val sessionTimeoutMillis: Long,
) : AppStateChangeListener, UserListener {

    private val sessionListeners = CopyOnWriteArrayList<SessionListener>()

    val requiredSession: Session get() = currentSession ?: Session.UNKNOWN

    var currentSession: Session? = null
        private set

    var lastEventTime: Long? = null
        private set

    fun initialize() {
        loadSession()
        loadLastEventTime()
        log.debug { "SessionManager initialized." }
    }

    fun addListener(listener: SessionListener) {
        sessionListeners.add(listener)
        log.debug { "SessionListener added [${listener::class.java.simpleName}]" }
    }

    fun startNewSession(user: User, timestamp: Long): Session {
        endSession(user)
        return newSession(user, timestamp)
    }

    fun startNewSessionIfNeeded(user: User, timestamp: Long): Session {

        val lastEventTime = lastEventTime ?: return startNewSession(user, timestamp)

        val currentSession = currentSession
        return if (currentSession != null && timestamp - lastEventTime < sessionTimeoutMillis) {
            updateLastEventTime(timestamp)
            currentSession
        } else {
            startNewSession(user, timestamp)
        }
    }

    fun updateLastEventTime(timestamp: Long) {
        lastEventTime = timestamp
        keyValueRepository.putLong(LAST_EVENT_TIME_KEY, timestamp)
        log.debug { "LastEventTime updated [$timestamp]" }
    }

    private fun endSession(user: User) {
        val currentSession = currentSession ?: return
        val lastEventTime = lastEventTime ?: return

        for (listener in sessionListeners) {
            listener.onSessionEnded(currentSession, user, lastEventTime)
        }
        log.debug { "Session ended [${currentSession.id}]" }
    }

    private fun newSession(user: User, timestamp: Long): Session {
        val newSession = Session.create(timestamp)
        currentSession = newSession
        saveSession(newSession)

        updateLastEventTime(timestamp)

        for (listener in sessionListeners) {
            listener.onSessionStarted(newSession, user, timestamp)
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

    override fun onChanged(state: AppState, timestamp: Long) {
        return when (state) {
            FOREGROUND -> {
                startNewSessionIfNeeded(userManager.currentUser, timestamp)
                Unit
            }
            BACKGROUND -> {
                updateLastEventTime(timestamp)
                currentSession?.let { saveSession(it) }
                Unit
            }
        }
    }

    override fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long) {
        endSession(oldUser)
        newSession(newUser, timestamp)
    }

    companion object {
        private val log = Logger<SessionManager>()
        private const val SESSION_ID_KEY = "session_id"
        private const val LAST_EVENT_TIME_KEY = "last_event_time"
    }
}
