package io.hackle.android.internal.engagement

import android.app.Activity
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.lifecycle.Lifecycle
import io.hackle.android.internal.lifecycle.Lifecycle.*
import io.hackle.android.internal.lifecycle.LifecycleListener
import io.hackle.android.internal.screen.Screen
import io.hackle.android.internal.screen.ScreenListener
import io.hackle.android.internal.screen.ScreenManager
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.atomic.AtomicReference

internal class EngagementManager(
    private val userManager: UserManager,
    private val screenManager: ScreenManager,
    private val minimumEngagementDurationMillis: Long
) : ApplicationListenerRegistry<EngagementListener>(), ScreenListener, LifecycleListener {

    private val _lastEngagementTime = AtomicReference<Long?>()
    val lastEngagementTime: Long? get() = _lastEngagementTime.get()

    private fun startEngagement(timestamp: Long) {
        _lastEngagementTime.set(timestamp)
    }

    private fun endEngagement(screen: Screen, timestamp: Long) {
        val startTimestamp = _lastEngagementTime.getAndSet(timestamp) ?: return

        val engagementDurationMillis = timestamp - startTimestamp
        if (engagementDurationMillis < minimumEngagementDurationMillis) {
            return
        }

        val engagement = Engagement(userManager.currentUser, screen, engagementDurationMillis)
        publish(engagement, timestamp)
    }

    private fun publish(engagement: Engagement, timestamp: Long) {
        log.debug { "Publish EngagementEvent(engagement=${engagement})" }
        for (listener in listeners) {
            try {
                listener.onEngagement(engagement, timestamp)
            } catch (e: Throwable) {
                log.error { "Failed to handle user engagement: $e" }
            }
        }
    }

    override fun onScreenStarted(previousScreen: Screen?, currentScreen: Screen, timestamp: Long) {
        startEngagement(timestamp)
    }

    override fun onScreenEnded(screen: Screen, timestamp: Long) {
        endEngagement(screen, timestamp)
    }

    override fun onLifecycle(lifecycle: Lifecycle, activity: Activity, timestamp: Long) {
        return when (lifecycle) {
            RESUMED -> startEngagement(timestamp)
            PAUSED -> {
                val screen = screenManager.currentScreen ?: return
                endEngagement(screen, timestamp)
            }

            CREATED, STARTED, STOPPED, DESTROYED -> Unit
        }
    }

    companion object {
        private val log = Logger<EngagementManager>()
    }
}
