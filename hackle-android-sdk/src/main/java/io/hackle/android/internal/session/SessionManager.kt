package io.hackle.android.internal.session

import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleListener
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.user.identifierEquals
import io.hackle.sdk.common.HackleSessionPolicy
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger

internal class SessionManager(
    private val userManager: UserManager,
    private val keyValueRepository: KeyValueRepository,
    private val sessionPolicy: HackleSessionPolicy = HackleSessionPolicy.DEFAULT,
) : ApplicationListenerRegistry<SessionListener>(), ApplicationLifecycleListener, UserListener {

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

    fun startNewSession(oldUser: User, newUser: User, timestamp: Long): Session {
        endSession(oldUser)
        return newSession(newUser, timestamp)
    }

    fun startNewSessionIfNeeded(oldUser: User, newUser: User, timestamp: Long): Session {
        if (shouldStartNewSession(oldUser, newUser)) {
            return startNewSession(oldUser, newUser, timestamp)
        }

        val lastEventTime = lastEventTime ?: return startNewSession(oldUser, newUser, timestamp)
        val currentSession = currentSession
        return if (currentSession != null && timestamp - lastEventTime < sessionPolicy.timeoutMillis) {
            updateLastEventTime(timestamp)
            currentSession
        } else {
            startNewSession(oldUser, newUser, timestamp)
        }
    }

    fun startNewSessionIfNeeded(user: User, timestamp: Long, isBackground: Boolean): Session {
        return if (!isBackground) {
            updateLastEventTime(timestamp)
            requiredSession
        } else if (sessionPolicy.expireOnBackground) {
            startNewSessionIfNeeded(user, user, timestamp)
        } else {
            requiredSession
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

        log.debug { "onSessionEnded(currentSession=${currentSession.id})" }
        for (listener in listeners) {
            listener.onSessionEnded(currentSession, user, lastEventTime)
        }
        log.debug { "Session ended [${currentSession.id}]" }
    }

    private fun newSession(user: User, timestamp: Long): Session {
        val newSession = Session.create(timestamp)
        currentSession = newSession
        saveSession(newSession)

        updateLastEventTime(timestamp)

        log.debug { "onSessionStarted(newSession=${newSession.id})" }
        for (listener in listeners) {
            listener.onSessionStarted(newSession, user, timestamp)
        }
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

    override fun onForeground(timestamp: Long, isFromBackground: Boolean) {
        val currentUser = userManager.currentUser
        startNewSessionIfNeeded(currentUser, currentUser, timestamp)
    }

    override fun onBackground(timestamp: Long) {
        updateLastEventTime(timestamp)
        currentSession?.let { saveSession(it) }
    }

    override fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long) {
        startNewSessionIfNeeded(oldUser, newUser, timestamp)
    }

    private fun shouldStartNewSession(oldUser: User, newUser: User): Boolean {
        if (oldUser.identifierEquals(newUser)) return false
        val condition = sessionPolicy.persistCondition ?: return true
        return !condition.shouldPersist(oldUser, newUser)
    }

    companion object {
        private val log = Logger<SessionManager>()
        private const val SESSION_ID_KEY = "session_id"
        private const val LAST_EVENT_TIME_KEY = "last_event_time"
    }
}
