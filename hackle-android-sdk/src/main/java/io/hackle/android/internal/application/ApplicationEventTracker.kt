package io.hackle.android.internal.application

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.HackleCore


internal class ApplicationEventTracker(
    private val userManager: UserManager,
    private val core: HackleCore,
    private val device: Device
) : ApplicationStateListener {
    override fun onOpen(timestamp: Long) {
        onAppOpen(timestamp)
    }

    override fun onState(state: AppState, timestamp: Long) {
        when (state) {
           AppState.FOREGROUND -> onForeground(timestamp)
           AppState.BACKGROUND -> onBackground(timestamp)
        }
    }

    private fun onAppOpen(timestamp: Long) {
        val trackEvent = Event.builder(APP_OPEN_EVENT_KEY)
            .property("versionName", device.properties["versionName"])
            .property("versionCode", device.properties["versionCode"])
            .build()
        val hackleUser = userManager.resolve(null, HackleAppContext.DEFAULT)
        core.track(trackEvent, hackleUser, timestamp)
    }
    
    private fun onForeground(timestamp: Long) {
        val trackEvent = Event.builder(APP_FOREGROUND_EVENT_KEY)
            .property("versionName", device.properties["versionName"])
            .property("versionCode", device.properties["versionCode"])
            .build()
        val hackleUser = userManager.resolve(null, HackleAppContext.DEFAULT)
        core.track(trackEvent, hackleUser, timestamp)
    }
    
    private fun onBackground(timestamp: Long) {
        val trackEvent = Event.builder(APP_BACKGROUND_EVENT_KEY)
            .property("versionName", device.properties["versionName"])
            .property("versionCode", device.properties["versionCode"])
            .build()
        val hackleUser = userManager.resolve(null, HackleAppContext.DEFAULT)
        core.track(trackEvent, hackleUser, timestamp)
    }

    companion object {
        const val APP_OPEN_EVENT_KEY = "\$app_open"
        const val APP_FOREGROUND_EVENT_KEY = "\$app_foreground"
        const val APP_BACKGROUND_EVENT_KEY = "\$app_background"

    }
}
