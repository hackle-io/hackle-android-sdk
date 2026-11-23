package io.hackle.android.internal.push.token

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.session.Session
import io.hackle.android.internal.session.SessionListener
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock

internal class PushTokenManager(
    private val repository: KeyValueRepository,
    private val pushTokenFetcher: PushTokenFetcher,
    private val pushEventTracker: PushEventTracker,
    private val userManager: UserManager,
    private val clock: Clock,
) : SessionListener {

    val currentPushToken: PushToken?
        get() {
            val tokenValue = repository.getString(PUSH_TOKEN_KEY) ?: return null
            return PushToken.of(tokenValue)
        }

    fun initialize() {
        try {
            pushTokenFetcher.fetch { pushToken ->
                try {
                    onPushTokenFetched(pushToken)
                } catch (e: Throwable) {
                    log.warn { "Failed to handle fetched PushToken: $e" }
                }
            }
        } catch (e: Throwable) {
            log.warn { "Failed to fetch PushToken: $e" }
        }
    }

    private fun onPushTokenFetched(pushToken: PushToken?) {
        if (pushToken == null || pushToken == currentPushToken) {
            return
        }
        repository.putString(PUSH_TOKEN_KEY, pushToken.value)
        pushEventTracker.trackToken(pushToken, userManager.currentUser, clock.currentMillis())
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
