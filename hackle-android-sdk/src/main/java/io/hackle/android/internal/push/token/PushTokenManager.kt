package io.hackle.android.internal.push.token

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.session.Session
import io.hackle.android.internal.session.SessionListener
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger

internal class PushTokenManager(
    private val repository: KeyValueRepository,
    private val pushTokenFetcher: PushTokenFetcher,
    private val pushEventTracker: PushEventTracker,
) : SessionListener {

    val currentPushToken: PushToken?
        get() {
            val tokenValue = repository.getString(PUSH_TOKEN_KEY) ?: return null
            return PushToken.of(tokenValue)
        }

    fun initialize() {
        try {
            fetch()
        } catch (e: Throwable) {
            log.debug { "Failed to fetch PushToken: $e" }
        }
    }

    private fun fetch() {
        val pushToken = pushTokenFetcher.fetch() ?: return
        if (pushToken == currentPushToken) {
            return
        }
        repository.putString(PUSH_TOKEN_KEY, pushToken.value)
    }

    override fun onSessionStarted(session: Session, user: User, timestamp: Long) {
        val pushToken = currentPushToken ?: return
        pushEventTracker.trackToken(pushToken, user, timestamp)
    }

    override fun onSessionEnded(session: Session, user: User, timestamp: Long) {
        // do nothing
    }

    companion object {
        private const val PUSH_TOKEN_KEY = "fcm_token"
        private val log = Logger<PushTokenManager>()
    }
}
