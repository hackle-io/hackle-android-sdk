package io.hackle.android.internal.event.dedup

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.time.Clock

internal class RemoteConfigEventDedupDeterminer(
    dedupIntervalMillis: Long,
    clock: Clock = Clock.SYSTEM,
) : CachedUserEventDedupDeterminer<RemoteConfigEventDedupDeterminer.Key, UserEvent.RemoteConfig>(
    dedupIntervalMillis,
    clock,
) {

    override fun supports(event: UserEvent): Boolean {
        return event is UserEvent.RemoteConfig
    }

    override fun cacheKey(event: UserEvent.RemoteConfig): Key {
        return Key.from(event)
    }

    data class Key(
        val parameterId: Long,
        val valueId: Long?,
        val decisionReason: DecisionReason,
    ) : CachedUserEventDedupDeterminer.Key {

        companion object {
            fun from(remoteConfigEvent: UserEvent.RemoteConfig): Key {
                return Key(
                    parameterId = remoteConfigEvent.parameter.id,
                    valueId = remoteConfigEvent.valueId,
                    decisionReason = remoteConfigEvent.decisionReason
                )
            }
        }
    }
}
