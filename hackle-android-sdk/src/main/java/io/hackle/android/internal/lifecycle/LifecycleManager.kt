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

    enum class LifecycleState {
        FOREGROUND,
        BACKGROUND,
    }

    interface LifecycleStateListener {
        fun onState(state: LifecycleState, timeInMillis: Long)
    }

    private var _currentActivity: WeakReference<Activity>? = null
    override val currentActivity: Activity?
        get() = _currentActivity?.get()

    private var currentState: LifecycleState? = null
    private val listeners: MutableList<LifecycleStateListener> = CopyOnWriteArrayList()
    private val dispatchStarted = AtomicBoolean(false)

    fun registerActivityLifecycleCallbacks(context: Context) {
        val app = context.applicationContext as Application
        app.unregisterActivityLifecycleCallbacks(this)
        app.registerActivityLifecycleCallbacks(this)
    }

    fun dispatchStart(timeInMillis: Long = System.currentTimeMillis()) {
        if (!dispatchStarted.getAndSet(true)) {
            logger.debug { "Dispatch Start" }
            currentState?.let { dispatch(it, timeInMillis) }
        }
    }

    fun addStateListener(listener: LifecycleStateListener) {
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
        dispatch(LifecycleState.FOREGROUND)
    }

    override fun onActivityPaused(activity: Activity) {
        dispatch(LifecycleState.BACKGROUND)
    }

    private fun dispatch(state: LifecycleState, timeInMillis: Long = System.currentTimeMillis()) {
        currentState = state

        if (dispatchStarted.get()) {
            for (listener in listeners) {
                try {
                    listener.onState(state, timeInMillis)
                } catch (throwable: Throwable) {
                    logger.error { "Unexpected exception calling ${listener::class.java.simpleName}[$state]: $throwable" }
                }
                logger.debug { "Dispatched lifecycle state [$state:$timeInMillis]" }
            }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {}
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