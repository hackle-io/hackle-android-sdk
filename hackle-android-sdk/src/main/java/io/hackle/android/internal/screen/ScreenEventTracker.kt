package io.hackle.android.internal.screen

import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore

internal class ScreenEventTracker(
    private val userManager: UserManager,
    private val core: HackleCore
) : ScreenListener {
    override fun onScreenStarted(previousScreen: Screen?, currentScreen: Screen, user: User, timestamp: Long) {
        val event = Event.builder(SCREEN_VIEW_EVENT_KEY)
            .property("\$screen_name", currentScreen.name)
            .property("\$screen_class", currentScreen.type)
            .property("\$previous_screen_name", previousScreen?.name)
            .property("\$previous_screen_class", previousScreen?.type)
            .build()
        val hackleUser = userManager.resolve(user)
        core.track(event, hackleUser, timestamp)
    }

    override fun onScreenEnded(screen: Screen, user: User, timestamp: Long) {
        // do nothing
    }

    companion object {
        private const val SCREEN_VIEW_EVENT_KEY = "\$screen_view"
    }
}
