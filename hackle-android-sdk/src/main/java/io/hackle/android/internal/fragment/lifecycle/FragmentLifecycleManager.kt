package io.hackle.android.internal.fragment.lifecycle

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

internal class FragmentLifecycleManager(
    private val clock: Clock
) : ApplicationListenerRegistry<FragmentLifecycleListener>(), FragmentProvider {

    private var state: FragmentState = FragmentState.INACTIVE
    private var fragment: WeakReference<Fragment>? = null
    private var fragmentManager: WeakReference<FragmentManager>? = null
    private var executor: Executor? = null
    private val callbacks = FragmentLifecycleCallbacksImpl()

    override val currentFragment: Fragment? get() = fragment?.get()
    override val currentState: FragmentState get() = state

    fun setExecutor(executor: Executor) {
        this.executor = executor
    }

    fun publishStateIfNeeded() {
        val currentFragment = currentFragment ?: return
        if (currentState != FragmentState.ACTIVE) {
            return
        }
        // fragment가 존재하고 active 상태이면 resume
        publish(FragmentLifecycle.RESUMED, currentFragment)
    }

    fun registerTo(fragmentManager: FragmentManager) {
        // 이전 FragmentManager와 동일하면 중복 등록 방지
        if (this.fragmentManager?.get() == fragmentManager) {
            return
        }

        unregister()
        fragmentManager.registerFragmentLifecycleCallbacks(callbacks, true)
        this.fragmentManager = WeakReference(fragmentManager)
    }

    fun unregister() {
        fragmentManager?.get()?.unregisterFragmentLifecycleCallbacks(callbacks)
        fragmentManager = null
        fragment = null
        state = FragmentState.INACTIVE
    }

    private inner class FragmentLifecycleCallbacksImpl : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
            onLifecycle(FragmentLifecycle.ATTACHED, f)
        }

        override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
            onLifecycle(FragmentLifecycle.CREATED, f)
        }

        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            onLifecycle(FragmentLifecycle.VIEW_CREATED, f)
        }

        override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
            onLifecycle(FragmentLifecycle.STARTED, f)
        }

        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            onLifecycle(FragmentLifecycle.RESUMED, f)
        }

        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            onLifecycle(FragmentLifecycle.PAUSED, f)
        }

        override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
            onLifecycle(FragmentLifecycle.STOPPED, f)
        }

        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            onLifecycle(FragmentLifecycle.VIEW_DESTROYED, f)
        }

        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
            onLifecycle(FragmentLifecycle.DESTROYED, f)
        }

        override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
            onLifecycle(FragmentLifecycle.DETACHED, f)
        }
    }

    private fun onLifecycle(fragmentLifecycle: FragmentLifecycle, fragment: Fragment) {
        resolveCurrentFragment(fragmentLifecycle, fragment)
        publish(fragmentLifecycle, fragment)
    }

    private fun resolveCurrentFragment(fragmentLifecycle: FragmentLifecycle, fragment: Fragment) {
        when (fragmentLifecycle) {
            FragmentLifecycle.ATTACHED, FragmentLifecycle.CREATED, FragmentLifecycle.VIEW_CREATED, FragmentLifecycle.STARTED -> {
                setCurrentFragmentIfNeeded(fragment)
            }

            FragmentLifecycle.RESUMED -> {
                setCurrentFragmentIfNeeded(fragment)
                this.state = FragmentState.ACTIVE
            }

            FragmentLifecycle.PAUSED -> {
                this.state = FragmentState.INACTIVE
            }

            FragmentLifecycle.STOPPED, FragmentLifecycle.VIEW_DESTROYED -> {
                unsetCurrentFragmentIfNeeded(fragment)
            }

            FragmentLifecycle.DESTROYED, FragmentLifecycle.DETACHED -> Unit
        }
    }

    private fun setCurrentFragmentIfNeeded(fragment: Fragment) {
        if (fragment != currentFragment) {
            this.fragment = WeakReference(fragment)
        }
    }

    private fun unsetCurrentFragmentIfNeeded(fragment: Fragment) {
        if (fragment == currentFragment) {
            this.fragment = null
        }
    }

    private fun publish(fragmentLifecycle: FragmentLifecycle, fragment: Fragment) {
        execute {
            log.debug { "onLifecycle(lifecycle=$fragmentLifecycle, fragment=${fragment.javaClass.simpleName})" }
            val timestamp = clock.currentMillis()
            for (listener in listeners) {
                try {
                    listener.onLifecycle(fragmentLifecycle, fragment, timestamp)
                } catch (e: Throwable) {
                    log.error { "Failed to handle lifecycle [${listener.javaClass.simpleName}, $fragmentLifecycle]: $e" }
                }
            }
        }
    }

    private fun execute(block: () -> Unit) {
        val executor = executor
        if (executor != null) {
            executor.execute(block)
        } else {
            block()
        }
    }

    companion object {
        private val log = Logger<FragmentLifecycleManager>()
        private var INSTANCE: FragmentLifecycleManager? = null

        val instance: FragmentLifecycleManager
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: create().also {
                        INSTANCE = it
                    }
                }
            }

        private fun create(): FragmentLifecycleManager {
            val fragmentLifecycleManager = FragmentLifecycleManager(Clock.SYSTEM)
            return fragmentLifecycleManager
        }
    }
}
