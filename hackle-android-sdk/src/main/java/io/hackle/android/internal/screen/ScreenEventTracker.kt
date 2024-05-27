package io.hackle.android.internal.screen

import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.HackleCore

internal class ScreenEventTracker(
    private val userManager: UserManager,
    private val core: HackleCore
) : ScreenListener {
    override fun onScreenStarted(previousScreen: Screen?, currentScreen: Screen, timestamp: Long) {
        if (currentScreen == previousScreen) {
            return
        }
        val event = Event.builder(SCREEN_VIEW_EVENT_KEY)
            .property("\$screen_name", currentScreen.name)
            .property("\$screen_class", currentScreen.type)
            .property("\$previous_screen_name", previousScreen?.name)
            .property("\$previous_screen_class", previousScreen?.type)
            .build()
        val user = userManager.resolve(null)
        core.track(event, user, timestamp)
    }

    override fun onScreenEnded(screen: Screen, timestamp: Long) {
        // do nothing
    }

    companion object {
        private const val SCREEN_VIEW_EVENT_KEY = "\$screen_view"
    }
}
