package io.hackle.android.internal.event

import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CopyOnWriteArrayList

internal class UserEventPublisher {

    private val listeners = CopyOnWriteArrayList<UserEventListener>()

    fun add(listener: UserEventListener) {
        listeners.add(listener)
        listeners.iterator()
    }

    fun publish(event: UserEvent) {
        for (listener in listeners) {
            try {
                listener.onEvent(event)
            } catch (e: Exception) {
                log.error { "Failed to publish UserEvent [${listener.javaClass.simpleName}]: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<UserEventPublisher>()
    }
}
