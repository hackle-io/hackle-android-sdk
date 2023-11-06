package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.sdk.core.internal.log.Logger
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

internal object LifecycleManager
    : Application.ActivityLifecycleCallbacks,
    ActivityProvider,
    AppStateProvider
{

    private val logger = Logger<LifecycleManager>()

    private var _currentActivity: WeakReference<Activity>? = null
    override val currentActivity: Activity?
        get() = _currentActivity?.get()

    private var _currentState: AppState? = null
    override val currentState: AppState
        get() = _currentState ?: AppState.BACKGROUND

    private val listeners: MutableList<AppStateChangeListener> = CopyOnWriteArrayList()

    private val dispatchStarted = AtomicBoolean(false)

    fun dispatchStart() {
        if (!dispatchStarted.getAndSet(true)) {
            logger.debug { "Dispatch Start" }
            _currentState?.let { dispatch(it) }
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
        if (_currentActivity != activity) {
            _currentActivity = WeakReference(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (_currentActivity != activity) {
            _currentActivity = WeakReference(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity == _currentActivity) {
            _currentActivity = null
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (_currentActivity != activity) {
            _currentActivity = WeakReference(activity)
        }

        dispatchIfNeeded(AppState.FOREGROUND)
    }

    override fun onActivityPaused(activity: Activity) {
        dispatchIfNeeded(AppState.BACKGROUND)
    }

    private fun dispatchIfNeeded(state: AppState, timeInMillis: Long = System.currentTimeMillis()) {
        _currentState = state
        if (dispatchStarted.get()) {
            dispatch(state, timeInMillis)
        }
    }

    private fun dispatch(state: AppState, timeInMillis: Long = System.currentTimeMillis()) {
        _currentState = state

        for (listener in listeners) {
            try {
                listener.onChanged(state, timeInMillis)
            } catch (throwable: Throwable) {
                logger.error { "Unexpected exception calling ${listener::class.java.simpleName}[$state]: $throwable" }
            }
            logger.debug { "Dispatched app state [$state:$timeInMillis]" }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}