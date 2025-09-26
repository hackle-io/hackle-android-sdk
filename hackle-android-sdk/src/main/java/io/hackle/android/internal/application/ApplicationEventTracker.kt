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

    override fun onInstall(timestamp: Long) {
        val trackEvent = createEvent(APP_INSTALL_EVENT_KEY)
        track(trackEvent, timestamp)
    }

    override fun onUpdate(timestamp: Long) {
        val trackEvent = createEvent(APP_UPDATE_EVENT_KEY) { builder ->
            builder
                .property("previousVersionName", device.packageInfo.previousVersionName)
                .property("previousVersionCode", device.packageInfo.previousVersionCode)
        }
        track(trackEvent, timestamp)
    }

    override fun onOpen(timestamp: Long) {
        val trackEvent = createEvent(APP_OPEN_EVENT_KEY)
        track(trackEvent, timestamp)
    }

    override fun onState(state: AppState, timestamp: Long) {
        when (state) {
           AppState.FOREGROUND -> onForeground(timestamp)
           AppState.BACKGROUND -> onBackground(timestamp)
        }
    }

    private fun onForeground(timestamp: Long) {
        val trackEvent = createEvent(APP_FOREGROUND_EVENT_KEY)
        track(trackEvent, timestamp)
    }

    private fun onBackground(timestamp: Long) {
        val trackEvent = createEvent(APP_BACKGROUND_EVENT_KEY)
        track(trackEvent, timestamp)
    }
    
    private fun createEvent(
        eventKey: String,
        additionalProperties: ((Event.Builder) -> Event.Builder)? = null
    ): Event {
        val packageInfo = device.packageInfo
        var builder = Event.builder(eventKey)
            .property("versionName", packageInfo.versionName)
            .property("versionCode", packageInfo.versionCode)

        if (additionalProperties != null) {
            builder = additionalProperties(builder)
        }

        return builder.build()
    }

    private fun track(event: Event, timestamp: Long) {
        val hackleUser = userManager.resolve(null, HackleAppContext.DEFAULT)
        core.track(event, hackleUser, timestamp)
    }

    companion object {
        const val APP_INSTALL_EVENT_KEY = "\$app_install"
        const val APP_UPDATE_EVENT_KEY = "\$app_update"
        const val APP_OPEN_EVENT_KEY = "\$app_open"
        const val APP_FOREGROUND_EVENT_KEY = "\$app_foreground"
        const val APP_BACKGROUND_EVENT_KEY = "\$app_background"

    }
}
