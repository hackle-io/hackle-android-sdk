package io.hackle.android.internal.application

import io.hackle.android.internal.context.HackleAppContext
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
        val trackEvent = Event.builder(APP_INSTALL_EVENT_KEY)
            .property("versionName", device.packageInfo.versionName)
            .property("versionCode", device.packageInfo.versionCode)
            .build()
        track(trackEvent, timestamp)
    }

    override fun onUpdate(timestamp: Long) {
        val trackEvent = Event.builder(APP_UPDATE_EVENT_KEY)
            .property("versionName", device.packageInfo.versionName)
            .property("versionCode", device.packageInfo.versionCode)
            .property("previousVersionName", device.packageInfo.previousVersionName)
            .property("previousVersionCode", device.packageInfo.previousVersionCode)
            .build()
        track(trackEvent, timestamp)
    }

    override fun onForeground(timestamp: Long, isFromBackground: Boolean) {
        val trackEvent = Event.builder(APP_OPEN_EVENT_KEY)
            .property("isFromBackground", isFromBackground)
            .build()
        track(trackEvent, timestamp)
    }

    override fun onBackground(timestamp: Long) {
        val trackEvent = Event.builder(APP_BACKGROUND_EVENT_KEY)
            .build()
        track(trackEvent, timestamp)
    }

    private fun track(event: Event, timestamp: Long) {
        val hackleUser = userManager.resolve(null, HackleAppContext.DEFAULT)
        core.track(event, hackleUser, timestamp)
    }

    companion object {
        const val APP_INSTALL_EVENT_KEY = "\$app_install"
        const val APP_UPDATE_EVENT_KEY = "\$app_update"
        const val APP_OPEN_EVENT_KEY = "\$app_open"
        const val APP_BACKGROUND_EVENT_KEY = "\$app_background"

    }
}
