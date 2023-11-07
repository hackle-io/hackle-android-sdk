package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.sdk.core.internal.log.Logger
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

internal class LifecycleManager : Application.ActivityLifecycleCallbacks, ActivityProvider {

    enum class Event {
        ON_CREATE,
        ON_DESTROY,
        ON_START,
        ON_STOP,
        ON_RESUME,
        ON_PAUSE,
    }

    interface LifecycleEventListener {
        fun onEvent(event: Event, timeInMillis: Long)
    }

    private var _currentActivity: WeakReference<Activity>? = null
    override val currentActivity: Activity?
        get() = _currentActivity?.get()

    private var currentEvent: Event? = null
    private val listeners: MutableList<LifecycleEventListener> = CopyOnWriteArrayList()
    private val dispatchStarted = AtomicBoolean(false)

    fun registerActivityLifecycleCallbacks(context: Context) {
        val app = context.applicationContext as Application
        app.unregisterActivityLifecycleCallbacks(this)
        app.registerActivityLifecycleCallbacks(this)
    }

    fun dispatchStart(timeInMillis: Long = System.currentTimeMillis()) {
        if (!dispatchStarted.getAndSet(true)) {
            logger.debug { "Dispatch Start" }
            currentEvent?.let { dispatch(it, timeInMillis) }
        }
    }

    fun addEventListener(listener: LifecycleEventListener) {
        listeners += listener
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (_currentActivity != activity) {
            _currentActivity = WeakReference(activity)
        }
        dispatch(Event.ON_CREATE)
    }

    override fun onActivityDestroyed(activity: Activity) {
        dispatch(Event.ON_DESTROY)
    }

    override fun onActivityStarted(activity: Activity) {
        if (_currentActivity != activity) {
            _currentActivity = WeakReference(activity)
        }
        dispatch(Event.ON_START)
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity == _currentActivity) {
            _currentActivity = null
        }
        dispatch(Event.ON_STOP)
    }

    override fun onActivityResumed(activity: Activity) {
        if (_currentActivity != activity) {
            _currentActivity = WeakReference(activity)
        }

        dispatch(Event.ON_RESUME)
    }

    override fun onActivityPaused(activity: Activity) {
        dispatch(Event.ON_PAUSE)
    }

    private fun dispatch(event: Event, timeInMillis: Long = System.currentTimeMillis()) {
        currentEvent = event

        if (dispatchStarted.get()) {
            for (listener in listeners) {
                try {
                    listener.onEvent(event, timeInMillis)
                } catch (throwable: Throwable) {
                    logger.error { "Unexpected exception calling ${listener::class.java.simpleName}[$event]: $throwable" }
                }
                logger.debug { "Dispatched lifecycle event [$event:$timeInMillis]" }
            }
        } else {
            logger.debug { "Skipped dispatching lifecycle event [$event:$timeInMillis]" }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    companion object {

        private val logger = Logger<LifecycleManager>()
        private var _instance: LifecycleManager? = null

        fun getInstance(): LifecycleManager {
            return _instance ?: synchronized(this) {
                _instance ?: LifecycleManager().also {
                    _instance = it
                }
            }
        }
    }
}