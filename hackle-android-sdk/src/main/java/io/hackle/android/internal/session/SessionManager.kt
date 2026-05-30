package io.hackle.android.internal.session

import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleListener
import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleManager
import io.hackle.android.internal.application.lifecycle.ApplicationState
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.user.identifierEquals
import io.hackle.android.internal.utils.concurrent.ReentrantLocker
import io.hackle.sdk.common.HackleSessionPolicy
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger

internal class SessionManager(
    private val userManager: UserManager,
    private val keyValueRepository: KeyValueRepository,
    private val applicationLifecycleManager: ApplicationLifecycleManager,
    private val sessionPolicy: HackleSessionPolicy = HackleSessionPolicy.DEFAULT,
) : ApplicationListenerRegistry<SessionListener>(), ApplicationLifecycleListener, UserListener {

    // currentSession/lastEventTime은 lifecycle(메인) 스레드, eventExecutor 스레드, 이벤트 파이프라인 스레드에서
    // 동시에 접근된다. 단순 가시성은 @Volatile로, check-then-act(세션 생성 판정)의 원자성은 lock으로 보장한다.
    // 락으로 보호된 메서드가 서로를 중첩 호출하므로(예: startNewSessionIfNeeded -> startNewSession -> updateLastEventTime)
    // 재진입 가능한 락이 필요하다.
    private val locker = ReentrantLocker()

    val requiredSession: Session get() = currentSession ?: Session.UNKNOWN

    @Volatile
    var currentSession: Session? = null
        private set

    @Volatile
    var lastEventTime: Long? = null
        private set

    fun initialize() = locker.withLock {
        loadSession()
        loadLastEventTime()
        log.debug { "SessionManager initialized." }
    }

    fun startNewSession(oldUser: User, newUser: User, timestamp: Long): Session = locker.withLock {
        endSession(oldUser)
        newSession(newUser, timestamp)
    }

    fun startNewSessionIfNeeded(context: SessionContext): Session = locker.withLock {
        if (shouldStartNewSession(context)) {
            startNewSession(context.oldUser, context.newUser, context.timestamp)
        } else {
            updateLastEventTime(context.timestamp)
            requiredSession
        }
    }

    fun updateLastEventTime(timestamp: Long) = locker.withLock {
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

    private fun isTimeoutEnabled(context: SessionContext): Boolean {
        if (context.isApplicationStateChange) {
            return sessionPolicy.timeoutCondition.onApplicationStateChange
        }
        return when (applicationLifecycleManager.currentState) {
            ApplicationState.FOREGROUND -> sessionPolicy.timeoutCondition.onForeground
            ApplicationState.BACKGROUND -> sessionPolicy.timeoutCondition.onBackground
        }
    }

    private fun isSessionTimedOut(timestamp: Long): Boolean {
        val lastEventTime = lastEventTime ?: return true
        return timestamp - lastEventTime >= sessionPolicy.timeoutCondition.timeoutMillis
    }

    private fun shouldStartNewSession(context: SessionContext): Boolean {
        if (currentSession == null) return true

        if (!context.oldUser.identifierEquals(context.newUser)) {
            if (!sessionPolicy.persistCondition.shouldPersist(context.oldUser, context.newUser)) return true
        }

        return isTimeoutEnabled(context) && isSessionTimedOut(context.timestamp)
    }

    override fun onForeground(timestamp: Long, isFromBackground: Boolean) = locker.withLock {
        val currentUser = userManager.currentUser
        startNewSessionIfNeeded(SessionContext.of(currentUser, timestamp, isApplicationStateChange = true))
        Unit
    }

    override fun onBackground(timestamp: Long) = locker.withLock {
        updateLastEventTime(timestamp)
        currentSession?.let { saveSession(it) }
        Unit
    }

    override fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long) = locker.withLock {
        startNewSessionIfNeeded(SessionContext.of(oldUser, newUser, timestamp))
        Unit
    }

    companion object {
        private val log = Logger<SessionManager>()
        private const val SESSION_ID_KEY = "session_id"
        private const val LAST_EVENT_TIME_KEY = "last_event_time"
    }
}
