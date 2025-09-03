package io.hackle.android.internal.monitoring.metric

import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.common.decision.RemoteConfigDecision
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.metrics.Timer

object DecisionMetrics {

    fun experiment(sample: Timer.Sample, key: Long, decision: Decision) {
        val tags = hashMapOf(
            "key" to key.toString(),
            "variation" to decision.variation.name,
            "reason" to decision.reason.name
        )
        val timer = Metrics.timer("experiment.decision", tags)
        sample.stop(timer)
    }

    fun featureFlag(sample: Timer.Sample, key: Long, decision: FeatureFlagDecision) {
        val tags = hashMapOf(
            "key" to key.toString(),
            "on" to decision.isOn.toString(),
            "reason" to decision.reason.name
        )
        val timer = Metrics.timer("feature.flag.decision", tags)
        sample.stop(timer)
    }

    fun remoteConfig(sample: Timer.Sample, key: String, decision: RemoteConfigDecision<*>) {
        val tags = hashMapOf(
            "key" to key,
            "reason" to decision.reason.name
        )
        val timer = Metrics.timer("remote.config.decision", tags)
        sample.stop(timer)
    }
}
