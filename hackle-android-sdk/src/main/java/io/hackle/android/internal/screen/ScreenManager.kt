package io.hackle.android.internal.screen

import android.app.Activity
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.Lifecycle
import io.hackle.android.internal.lifecycle.Lifecycle.*
import io.hackle.android.internal.lifecycle.LifecycleListener
import io.hackle.android.internal.session.SessionManager
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.atomic.AtomicReference


internal class ScreenManager(
    private val activityProvider: ActivityProvider,
) : ApplicationListenerRegistry<ScreenListener>(), LifecycleListener {

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
        if (previousScreen != null) {
            publishEnd(previousScreen, timestamp)
        }
        publishStart(previousScreen, screen, timestamp)
    }

    private fun publishStart(previousScreen: Screen?, screen: Screen, timestamp: Long) {
        log.debug { "Publish ScreenStartEvent(previousScreen=${previousScreen}, screen=$screen)" }
        for (listener in listeners) {
            try {
                listener.onScreenStarted(previousScreen, screen, timestamp)
            } catch (e: Throwable) {
                log.error { "Failed to handle screen start [${listener.javaClass.simpleName}]: $e" }
            }
        }
    }

    private fun publishEnd(screen: Screen, timestamp: Long) {
        log.debug { "Publish ScreenEndEvent(screen=$screen)" }
        for (listener in listeners) {
            try {
                listener.onScreenEnded(screen, timestamp)
            } catch (e: Throwable) {
                log.error { "Failed to handle screen end [${listener.javaClass.simpleName}]: $e" }
            }
        }
    }

    override fun onLifecycle(lifecycle: Lifecycle, activity: Activity, timestamp: Long) {
        return when (lifecycle) {
            RESUMED -> updateScreen(Screen.from(activity), timestamp)
            PAUSED, CREATED, STARTED, STOPPED, DESTROYED -> Unit
        }
    }

    companion object {
        private val log = Logger<SessionManager>()
    }
}
