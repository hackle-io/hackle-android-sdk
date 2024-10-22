package io.hackle.android.internal.event.dedup

import io.hackle.android.internal.database.repository.AndroidKeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateListener
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.time.Clock

internal class RemoteConfigEventDedupDeterminer(
    repository: AndroidKeyValueRepository,
    dedupIntervalMillis: Long,
    clock: Clock = Clock.SYSTEM,
): AppStateListener, CachedUserEventDedupDeterminer<RemoteConfigEventDedupDeterminer.Key, UserEvent.RemoteConfig>(
    repository,
    dedupIntervalMillis,
    clock,
) {

    override fun supports(event: UserEvent): Boolean {
        return event is UserEvent.RemoteConfig
    }

    override fun cacheKey(event: UserEvent.RemoteConfig): String {
        return key(event)
    }

    private fun key(event: UserEvent.RemoteConfig): String {
        val key = Key.from(event)
        return listOf("${key.parameterId}", "${key.valueId}", key.decisionReason).joinToString("-")
    }

    override fun onState(state: AppState, timestamp: Long) {
        if (state == AppState.BACKGROUND) {
            saveToRepository()
        }
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
