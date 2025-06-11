package io.hackle.android.internal.event

import io.hackle.sdk.common.HackleCommonEvent
import io.hackle.sdk.common.PropertiesBuilder

internal data class InternalEvent(
    override val key: String,
    override val value: Double?,
    override val properties: Map<String, Any>,
    override val internalProperties: Map<String, Any>?
): HackleCommonEvent {
    class Builder(key: String) : HackleCommonEvent.HackleCommonEventBuilder<InternalEvent>(key) {
        fun internalProperty(key: String, value: Any?) = apply {
            if (internalProperties == null) {
                internalProperties = PropertiesBuilder()
            }
            internalProperties?.add(key, value)
        }

        override fun build(): InternalEvent = InternalEvent(key, value, properties.build(), internalProperties?.build())
    }

    companion object {
        fun builder(key: String) = Builder(key)
    }
}
