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
            .property(SCREEN_NAME_PROPERTY_KEY, currentScreen.name)
            .property(SCREEN_CLASS_PROPERTY_KEY, currentScreen.className)
            .property(PREVIOUS_SCREEN_NAME_PROPERTY_KEY, previousScreen?.name)
            .property(PREVIOUS_SCREEN_CLASS_PROPERTY_KEY, previousScreen?.className)
            .build()
        val hackleUser = userManager.toHackleUser(user)
        core.track(event, hackleUser, timestamp)
    }

    override fun onScreenEnded(screen: Screen, user: User, timestamp: Long) {
        // do nothing
    }

    companion object {
        const val SCREEN_VIEW_EVENT_KEY = "\$page_view"
        const val SCREEN_NAME_PROPERTY_KEY = "\$page_name"
        const val SCREEN_CLASS_PROPERTY_KEY = "\$page_class"
        const val PREVIOUS_SCREEN_NAME_PROPERTY_KEY = "\$previous_page_name"
        const val PREVIOUS_SCREEN_CLASS_PROPERTY_KEY = "\$previous_page_class"
    }
}
