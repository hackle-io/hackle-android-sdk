package io.hackle.android.internal.pii

import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertyOperations

internal enum class PIIProperty(val key: String) {
    PHONE_NUMBER("\$phone_number")
}

internal fun PropertyOperations.toSecuredEvent(): Event {
    val builder = Event.builder("\$secured_properties")
    for ((operation, properties) in asMap()) {
        builder.property(operation.key, properties)
    }
    return builder.build()
}