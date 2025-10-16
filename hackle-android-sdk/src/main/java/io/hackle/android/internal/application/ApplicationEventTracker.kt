package io.hackle.android.internal.application

import io.hackle.android.internal.application.install.ApplicationInstallStateListener
import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleListener
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.HackleCore

internal class ApplicationEventTracker(
    private val userManager: UserManager,
    private val core: HackleCore
) : ApplicationLifecycleListener, ApplicationInstallStateListener {

    override fun onInstall(versionInfo: PackageVersionInfo, timestamp: Long) {
        val trackEvent = Event.builder(APP_INSTALL_EVENT_KEY)
            .property("version_name", versionInfo.versionName)
            .property("version_code", versionInfo.versionCode)
            .build()
        track(trackEvent, timestamp)
    }

    override fun onUpdate(previousVersion: PackageVersionInfo?, currentVersion: PackageVersionInfo, timestamp: Long) {
        val trackEvent = Event.builder(APP_UPDATE_EVENT_KEY)
            .property("version_name", currentVersion.versionName)
            .property("version_code", currentVersion.versionCode)
            .property("previous_version_name", previousVersion?.versionName)
            .property("previous_version_code", previousVersion?.versionCode)
            .build()
        track(trackEvent, timestamp)
    }

    override fun onForeground(timestamp: Long, isFromBackground: Boolean) {
        val trackEvent = Event.builder(APP_OPEN_EVENT_KEY)
            .property("is_from_background", isFromBackground)
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
