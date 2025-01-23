package io.hackle.android.internal.user

import io.hackle.sdk.core.model.Identifier
import io.hackle.sdk.core.model.TargetEvent

internal data class UserTargetEvent(
    val identifier: Identifier,
    val targetEvents: List<TargetEvent>
)

internal data class UserTargetEvents internal constructor(private val targetEvents: Map<Identifier, UserTargetEvent>) {

    operator fun get(identifier: Identifier): UserTargetEvent? {
        return targetEvents[identifier]
    }

    fun asList(): List<UserTargetEvent> {
        return targetEvents.values.toList()
    }

    fun asMap(): Map<Identifier, UserTargetEvent> {
        return targetEvents
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder internal constructor() {

        private val targetEvents = hashMapOf<Identifier, UserTargetEvent>()

        constructor(targetEvents: UserTargetEvents) : this() {
            putAll(targetEvents.asList())
        }

        fun put(targetEvent: UserTargetEvent) = apply {
            targetEvents[targetEvent.identifier] = targetEvent
        }

        fun putAll(targetEvents: List<UserTargetEvent>) = apply {
            for (targetEvent in targetEvents) {
                put(targetEvent)
            }
        }

        fun putAll(targetEvents: UserTargetEvents) = apply {
            putAll(targetEvents.asList())
        }

        fun build(): UserTargetEvents {
            return from(targetEvents)
        }
    }

    companion object {
        private val EMPTY = UserTargetEvents(emptyMap())

        fun empty(): UserTargetEvents {
            return EMPTY
        }

        fun from(targetEvents: Map<Identifier, UserTargetEvent>): UserTargetEvents {
            return UserTargetEvents(targetEvents)
        }
    }
}
