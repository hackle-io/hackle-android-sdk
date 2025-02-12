package io.hackle.android.internal.user

import io.hackle.sdk.core.model.TargetEvent


internal data class UserTargetEvents internal constructor(private val targetEvents: List<TargetEvent>) {

    fun asList(): List<TargetEvent> {
        return targetEvents
    }

    class Builder internal constructor() {

        private val targetEvents = mutableListOf<TargetEvent>()

        constructor(targetEvents: UserTargetEvents) : this() {
            putAll(targetEvents)
        }

        fun put(targetEvent: TargetEvent) = apply {
            targetEvents.add(targetEvent)
        }

        fun putAll(targetEvents: List<TargetEvent>) = apply {
            for (targetEvent in targetEvents) {
                put(targetEvent)
            }
        }

        fun putAll(targetEvents: UserTargetEvents) = apply {
            putAll(targetEvents.targetEvents)
        }

        fun build(): UserTargetEvents {
            return from(targetEvents)
        }
    }

    companion object {
        private val EMPTY = UserTargetEvents(emptyList())

        fun empty(): UserTargetEvents {
            return EMPTY
        }

        fun builder(): Builder {
            return Builder()
        }

        fun from(targetEvents: List<TargetEvent>): UserTargetEvents {
            return UserTargetEvents(targetEvents)
        }

        fun from(dto: UserTargetResponseDto): UserTargetEvents {
            return dto.events
                .fold(builder(), Builder::put)
                .build()
        }
    }
}
