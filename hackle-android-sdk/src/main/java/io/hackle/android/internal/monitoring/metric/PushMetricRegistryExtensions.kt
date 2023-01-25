package io.hackle.android.internal.monitoring.metric

import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.android.internal.lifecycle.HackleActivityLifecycleCallbacks
import io.hackle.android.internal.lifecycle.listen
import io.hackle.sdk.core.internal.metrics.push.PushMetricRegistry

internal fun HackleActivityLifecycleCallbacks.add(registry: PushMetricRegistry) = apply {
    registry.listen(this)
}

internal fun <T : PushMetricRegistry> T.listen(callbacks: HackleActivityLifecycleCallbacks): T =
    apply { PushMetricAppStateChangeListener(this).listen(callbacks) }

private class PushMetricAppStateChangeListener(
    private val registry: PushMetricRegistry,
) : AppStateChangeListener {
    override fun onChanged(state: AppState, timestamp: Long) {
        return when (state) {
            AppState.FOREGROUND -> registry.start()
            AppState.BACKGROUND -> registry.stop()
        }
    }

    override fun toString(): String {
        return registry.toString()
    }
}
