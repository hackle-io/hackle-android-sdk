package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.android.ui.HackleActivity
import io.hackle.sdk.core.internal.log.Logger
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

internal class LifecycleManager : Application.ActivityLifecycleCallbacks, ActivityProvider {

    enum class LifecycleState {
        FOREGROUND,
        BACKGROUND,
    }

    interface LifecycleStateListener {
        fun onState(state: LifecycleState, timestamp: Long)
    }

    private var _currentActivity: WeakReference<Activity>? = null
    override var currentActivity: Activity?
        get() = _currentActivity?.get()
        private set(newValue) {
            _currentActivity = WeakReference(newValue)
        }

    private var currentState: LifecycleState? = null
    private val listeners: MutableList<LifecycleStateListener> = CopyOnWriteArrayList()

    fun registerActivityLifecycleCallbacks(context: Context) {
        val app = context.applicationContext as Application
        app.unregisterActivityLifecycleCallbacks(this)
        app.registerActivityLifecycleCallbacks(this)
    }

    fun repeatCurrentState(timestamp: Long = System.currentTimeMillis()) {
        currentState?.let {
            logger.debug { "Repeat current lifecycle state [$it:$timestamp]" }
            dispatchForce(it, timestamp)
        }
    }

    fun addStateListener(listener: LifecycleStateListener) {
        listeners += listener
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (currentActivity != activity && activity !is HackleActivity) {
            currentActivity = activity
        }
    }


    override fun onActivityStarted(activity: Activity) {
        if (currentActivity != activity && activity !is HackleActivity) {
            currentActivity = activity
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity == currentActivity) {
            currentActivity = null
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (currentActivity != activity && activity !is HackleActivity) {
            currentActivity = activity
        }
        dispatch(LifecycleState.FOREGROUND)
    }

    override fun onActivityPaused(activity: Activity) {
        dispatch(LifecycleState.BACKGROUND)
    }

    private fun dispatch(state: LifecycleState, timestamp: Long = System.currentTimeMillis()) {
        currentState = state
        dispatchForce(state, timestamp)
    }

    private fun dispatchForce(state: LifecycleState, timestamp: Long = System.currentTimeMillis()) {
        for (listener in listeners) {
            try {
                listener.onState(state, timestamp)
            } catch (throwable: Throwable) {
                logger.error { "Unexpected exception calling ${listener::class.java.simpleName}[$state]: $throwable" }
            }
            logger.debug { "Dispatched lifecycle state [$state:$timestamp]" }
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