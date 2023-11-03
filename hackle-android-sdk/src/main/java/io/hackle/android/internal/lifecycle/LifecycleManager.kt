package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.sdk.core.internal.log.Logger
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal object LifecycleManager
    : Application.ActivityLifecycleCallbacks,
    AppStateProvider,
    ActivityProvider
{

    private val logger = Logger<LifecycleManager>()

    private var _currentActivity: WeakReference<Activity>? = null
    override val currentActivity: Activity?
        get() = _currentActivity?.get()

    private var _lastStateChangeTimeInMillis: Long = 0L
    private var _currentState: AppState? = null
    override val currentState: AppState
        get() = _currentState ?: AppState.BACKGROUND

    private val dispatchStarted = AtomicBoolean(false)

    private val appInLaunched = AtomicBoolean(false)
    private val appInForegrounded = AtomicBoolean(false)

    private val activityCreatedCount = AtomicInteger(0)
    private val activityStartedCount = AtomicInteger(0)

    private val listeners: MutableList<AppStateChangeListener> = CopyOnWriteArrayList()

    fun dispatchStart() {
        if (!dispatchStarted.getAndSet(true)) {
            _currentState?.let {
                dispatch(it, System.currentTimeMillis())
            }
        }
    }

    fun registerActivityLifecycleCallbacks(context: Context) {
        val app = context.applicationContext as Application
        app.unregisterActivityLifecycleCallbacks(this)
        app.registerActivityLifecycleCallbacks(this)
    }

    fun addListener(listener: AppStateChangeListener) {
        listeners += listener
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        appInLaunched.set(true)
        activityCreatedCount.increment()
        if (_currentActivity != activity) {
            _currentActivity = WeakReference(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityCreatedCount.decrement()
    }

    override fun onActivityStarted(activity: Activity) {
        activityStartedCount.increment()
    }

    override fun onActivityStopped(activity: Activity) {
        if (appInLaunched.get() &&
            activityStartedCount.decrementAndGet() <= 0 &&
            !activity.isChangingConfigurations) {
            appInForegrounded.getAndSet(false)
            dispatchIfNeeded(AppState.BACKGROUND)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (appInLaunched.get() &&
            !appInForegrounded.getAndSet(true) &&
            !activity.isChangingConfigurations) {
            dispatchIfNeeded(AppState.FOREGROUND)
        }
    }

    private fun dispatchIfNeeded(state: AppState, timeInMillis: Long = System.currentTimeMillis()) {
        if (_currentState == state) {
            return
        }

        _currentState = state
        _lastStateChangeTimeInMillis = timeInMillis

        logger.debug { "Changed app state [$state:$timeInMillis]" }

        if (dispatchStarted.get()) {
            dispatch(state, timeInMillis)
        }
    }

    private fun dispatch(state: AppState, timeInMillis: Long) {
        for (listener in listeners) {
            try {
                listener.onChanged(state, timeInMillis)
            } catch (throwable: Throwable) {
                logger.error { "Unexpected exception calling ${listener::class.java.simpleName}[$state]: $throwable" }
            }
        }
        logger.debug { "Dispatched app state [$state:$timeInMillis]" }
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    private fun AtomicInteger.increment() {
        incrementAndGet()
    }

    private fun AtomicInteger.decrement() {
        decrementAndGet()
    }
}