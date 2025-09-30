package io.hackle.android.internal.event.dedup

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateListener
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.time.Clock

internal class ExposureEventDedupDeterminer(
    repository: KeyValueRepository,
    dedupIntervalMillis: Long,
    clock: Clock = Clock.SYSTEM
): AppStateListener, CachedUserEventDedupDeterminer<ExposureEventDedupDeterminer.Key, UserEvent.Exposure>(
    repository,
    dedupIntervalMillis,
    clock
) {
    override fun supports(event: UserEvent): Boolean {
        return event is UserEvent.Exposure
    }

    override fun cacheKey(event: UserEvent.Exposure): String {
        return key(event)
    }

    private fun key(event: UserEvent.Exposure): String {
        val key = Key.from(event)
        return listOf("${key.experimentId}", "${key.variationId}", key.variationKey, key.decisionReason).joinToString("-")
    }

    override fun onBackground(timestamp: Long) {
        saveToRepository()
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
