package io.hackle.android.internal.pii

import io.hackle.android.internal.pii.phonenumber.PhoneNumber
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.core.HackleCore

internal class PIIEventManager(
    private val userManager: UserManager,
    private val core: HackleCore,
) {
    fun setPhoneNumber(phoneNumber: PhoneNumber, timestamp: Long) {
        val properties = PropertyOperations.builder()
            .set(PIIProperty.PHONE_NUMBER.key, phoneNumber.value)
            .build()
        val event = properties.toSecuredEvent()
        track(event, timestamp)
    }

    fun unsetPhoneNumber(timestamp: Long) {
        val properties = PropertyOperations.builder()
            .unset(PIIProperty.PHONE_NUMBER.key)
            .build()
        val event = properties.toSecuredEvent()
        track(event, timestamp)
    }

    private fun track(event: Event, timestamp: Long) {
        val hackleUser = userManager.resolve(null)
        core.track(event, hackleUser, timestamp)
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