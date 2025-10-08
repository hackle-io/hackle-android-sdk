package io.hackle.android.internal.screen

import android.app.Activity
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.activity.lifecycle.ActivityProvider
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycle
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycle.*
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycleListener
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Screen
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.atomic.AtomicReference


internal class ScreenManager(
    private val userManager: UserManager,
    private val activityProvider: ActivityProvider,
) : ApplicationListenerRegistry<ScreenListener>(), ActivityLifecycleListener {

    private val _currentScreen = AtomicReference<Screen?>()
    val currentScreen: Screen? get() = _currentScreen.get()

    fun setCurrentScreen(screen: Screen, timestamp: Long) {
        updateScreen(screen, timestamp)
    }

    fun resolveScreenClass(screenClass: String? = null): String {
        if (screenClass != null) {
            return screenClass
        }
        val activity = activityProvider.currentActivity ?: return "Unknown"
        return activity.javaClass.simpleName
    }

    private fun updateScreen(screen: Screen, timestamp: Long) {
        val previousScreen = _currentScreen.getAndSet(screen)
        if (screen == previousScreen) {
            return
        }
        val user = userManager.currentUser
        if (previousScreen != null) {
            publishEnd(previousScreen, user, timestamp)
        }
        publishStart(previousScreen, screen, user, timestamp)
    }

    private fun publishStart(previousScreen: Screen?, screen: Screen, user: User, timestamp: Long) {
        log.debug { "onScreenStarted(previousScreen=${previousScreen}, screen=$screen)" }
        for (listener in listeners) {
            try {
                listener.onScreenStarted(previousScreen, screen, user, timestamp)
            } catch (e: Throwable) {
                log.error { "Failed to handle screen start [${listener.javaClass.simpleName}]: $e" }
            }
        }
    }

    private fun publishEnd(screen: Screen, user: User, timestamp: Long) {
        log.debug { "onScreenEnded(screen=$screen)" }
        for (listener in listeners) {
            try {
                listener.onScreenEnded(screen, user, timestamp)
            } catch (e: Throwable) {
                log.error { "Failed to handle screen end [${listener.javaClass.simpleName}]: $e" }
            }
        }
    }

    override fun onLifecycle(activityLifecycle: ActivityLifecycle, activity: Activity, timestamp: Long) {
        return when (activityLifecycle) {
            RESUMED -> updateScreen(Screen.from(activity), timestamp)
            PAUSED, CREATED, STARTED, STOPPED, DESTROYED -> Unit
        }
    }

    companion object {
        private val log = Logger<SessionManager>()
    }
}
