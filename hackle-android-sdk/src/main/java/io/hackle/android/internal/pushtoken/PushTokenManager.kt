package io.hackle.android.internal.pushtoken

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.pushtoken.registration.PushTokenRegistration
import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.log.Logger

internal class PushTokenManager(
    private val core: HackleCore,
    private val preferences: KeyValueRepository,
    private val userManager: UserManager,
    private val registration: PushTokenRegistration
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
            val pushToken = registration.getPushToken()
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

        val event = RegisterPushTokenEvent(pushToken).toTrackEvent()
        track(event, user, timestamp)
    }

    private fun track(event: Event, user: User, timestamp: Long) {
        val hackleUser = userManager.toHackleUser(user)
        core.track(event, hackleUser, timestamp)
        log.debug { "${event.key} event queued." }
    }

    companion object {

        private const val KEY_PUSH_TOKEN = "fcm_token"
        private val log = Logger<PushTokenManager>()
    }
}