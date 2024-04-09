package io.hackle.android.internal.push

import io.hackle.android.internal.pushtoken.datasource.ProviderType
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.event.UserEvent

internal class PushEventTracker(
    private val userManager: UserManager,
    private val core: HackleCore,
) {

    fun trackToken(token: String, user: User, timestamp: Long) {
        val event = Event.builder(PUSH_TOKEN_EVENT_NAME)
            .property("provider_type", ProviderType.FCM.name)
            .property("token", token)
            .build()
        track(event, user, timestamp)
    }

    private fun track(event: Event, user: User, timestamp: Long) {
        val hackleUser = userManager.toHackleUser(user)
        core.track(event, hackleUser, timestamp)
    }

    companion object {
        private const val PUSH_TOKEN_EVENT_NAME = "\$push_token"

        fun isPushTokenEvent(event: UserEvent): Boolean {
            val trackEvent = event as? UserEvent.Track ?: return false
            return trackEvent.event.key == PUSH_TOKEN_EVENT_NAME
        }
    }
}