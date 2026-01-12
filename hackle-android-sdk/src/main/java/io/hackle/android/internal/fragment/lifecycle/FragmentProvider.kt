package io.hackle.android.internal.fragment.lifecycle

import androidx.fragment.app.Fragment

internal interface FragmentProvider {
    val currentState: FragmentState
    val currentFragment: Fragment?
}

internal enum class FragmentState {
    ACTIVE,
    INACTIVE
}
