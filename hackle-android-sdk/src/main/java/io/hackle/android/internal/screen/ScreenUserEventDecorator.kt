package io.hackle.android.internal.screen

import io.hackle.android.internal.event.UserEventDecorator
import io.hackle.sdk.core.event.UserEvent

internal class ScreenUserEventDecorator(
    private val screenManager: ScreenManager,
) : UserEventDecorator {
    override fun decorate(event: UserEvent): UserEvent {
        val screen = screenManager.currentScreen ?: return event

        val newUser = event.user.toBuilder()
            .hackleProperty("screenName", screen.name)
            .hackleProperty("screenClass", screen.className)
            .build()

        return event.with(newUser)
    }
}
