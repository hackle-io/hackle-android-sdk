package io.hackle.android.internal.fragment.lifecycle

import androidx.fragment.app.Fragment
import io.hackle.android.internal.core.listener.ApplicationListener

internal interface FragmentLifecycleListener : ApplicationListener {
    fun onLifecycle(fragmentLifecycle: FragmentLifecycle, fragment: Fragment, timestamp: Long)
}
