package io.hackle.android.internal.optout

internal class OptOutState(configOptOutTracking: Boolean) {
    @Volatile
    var isOptOutTracking: Boolean = configOptOutTracking
        internal set
}
