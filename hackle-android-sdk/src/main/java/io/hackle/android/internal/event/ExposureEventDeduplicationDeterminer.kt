package io.hackle.android.internal.event

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.user.HackleUser

internal class ExposureEventDeduplicationDeterminer(
    private val exposureEventDedupIntervalMillis: Long?,
) {

    private var cache = hashMapOf<Key, Long>()
    private var currentUser: HackleUser? = null

    fun isDeduplicationTarget(event: UserEvent): Boolean {
        if (exposureEventDedupIntervalMillis == null) {
            return false
        }

        val exposureEvent = event as? UserEvent.Exposure ?: return false

        if (event.user.identifiers != currentUser?.identifiers) {
            currentUser = event.user
            cache = hashMapOf()
        }

        val key = Key.from(exposureEvent)
        val now = System.currentTimeMillis()

        val firstExposureTimeMillis = cache[key]
        if (firstExposureTimeMillis != null && firstExposureTimeMillis >= now - exposureEventDedupIntervalMillis) {
            return true
        }

        cache[key] = now
        return false
    }

    data class Key(
        val experimentId: Long,
        val variationId: Long?,
        val variationKey: String,
        val decisionReason: DecisionReason,
    ) {
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
