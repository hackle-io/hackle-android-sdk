package io.hackle.android.internal.event.dedup

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.time.Clock

internal class ExposureEventDedupDeterminer(
    dedupIntervalMillis: Long,
    clock: Clock = Clock.SYSTEM
) : CachedUserEventDedupDeterminer<ExposureEventDedupDeterminer.Key, UserEvent.Exposure>(
    dedupIntervalMillis,
    clock
) {
    override fun supports(event: UserEvent): Boolean {
        return event is UserEvent.Exposure
    }

    override fun cacheKey(event: UserEvent.Exposure): Key {
        return Key.from(event)
    }

    data class Key(
        val experimentId: Long,
        val variationId: Long?,
        val variationKey: String,
        val decisionReason: DecisionReason,
    ) : CachedUserEventDedupDeterminer.Key {
        companion object {
            fun from(exposureEvent: UserEvent.Exposure): Key {
                return Key(
                    experimentId = exposureEvent.experiment.id,
                    variationId = exposureEvent.variationId,
                    variationKey = exposureEvent.variationKey,
                    decisionReason = exposureEvent.decisionReason,
                )
            }
        }
    }
}