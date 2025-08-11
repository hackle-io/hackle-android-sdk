package io.hackle.android.internal.pii

import io.hackle.android.internal.pii.phonenumber.PhoneNumber
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.core.HackleCore

internal class PIIEventManager {
    fun setPhoneNumber(phoneNumber: PhoneNumber): Event {
        val properties = PropertyOperations.builder()
            .set(PIIProperty.PHONE_NUMBER.key, phoneNumber.value)
            .build()
        return properties.toSecuredEvent()
    }

    fun unsetPhoneNumber(): Event {
        val properties = PropertyOperations.builder()
            .unset(PIIProperty.PHONE_NUMBER.key)
            .build()
        return properties.toSecuredEvent()
    }
}

internal enum class PIIProperty(val key: String) {
    PHONE_NUMBER("\$phone_number")
}

private fun PropertyOperations.toSecuredEvent(): Event {
    val builder = Event.builder("\$secured_properties")
    for ((operation, properties) in asMap()) {
        builder.property(operation.key, properties)
    }
    return builder.build()
}