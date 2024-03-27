package io.hackle.android.internal.pushtoken

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.pushtoken.datasource.PushTokenDataSource
import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger

internal class PushTokenManager(
    private val preferences: KeyValueRepository,
    private val userManager: UserManager,
    private val dataSource: PushTokenDataSource,
    private val eventTracker: PushEventTracker,
) : UserListener {

    private var _registeredPushToken: String?
        get() = preferences.getString(KEY_PUSH_TOKEN)
        private set(value) {
            if (value == null) {
                preferences.remove(KEY_PUSH_TOKEN)
            } else {
                preferences.putString(KEY_PUSH_TOKEN, value)
            }
        }

    val registeredPushToken: String?
        get() = _registeredPushToken

    fun initialize() {
        updatePushToken()
    }

    override fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long) {
        notifyPushTokenChanged(newUser, timestamp)
    }

    private fun updatePushToken() {
        try {
            val timestamp = System.currentTimeMillis()
            val currentUser = userManager.currentUser
            val pushToken = dataSource.getPushToken()
            if (pushToken.isNullOrEmpty() || _registeredPushToken == pushToken) {
                return
            }

            _registeredPushToken = pushToken
            notifyPushTokenChanged(currentUser, timestamp)
        } catch (e: Exception) {
            log.debug { "Failed to register push token: $e" }
        }
    }

    private fun notifyPushTokenChanged(user: User, timestamp: Long) {
        val pushToken = _registeredPushToken
        if (pushToken.isNullOrEmpty()) {
            log.debug { "Push token is empty." }
            return
        }

        eventTracker.trackToken(pushToken, user, timestamp)
    }

    companion object {
        private const val KEY_PUSH_TOKEN = "fcm_token"
        private val log = Logger<PushTokenManager>()
    }
}