package io.hackle.android.internal.pii

import io.hackle.android.internal.pii.phonenumber.PhoneNumber
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.log.Logger

internal class PIIEventManager(
    private val userManager: UserManager,
    private val core: HackleCore,
) {
    fun setPhoneNumber(phoneNumber: String, user: User, timestamp: Long) {
        val filteredPhoneNumber = PhoneNumber.filtered(phoneNumber)
        val properties = PropertyOperations.builder()
            .set(PIIProperty.PHONE_NUMBER.key, filteredPhoneNumber)
            .build()
        val event = properties.toSecuredEvent()
        track(event, user, timestamp)
    }

    fun unsetPhoneNumber(user: User, timestamp: Long) {
        val properties = PropertyOperations.builder()
            .unset(PIIProperty.PHONE_NUMBER.key)
            .build()
        val event = properties.toSecuredEvent()
        track(event, user, timestamp)
    }

    private fun track(event: Event, user: User, timestamp: Long) {
        val hackleUser = userManager.toHackleUser(user)
        core.track(event, hackleUser, timestamp)
    }

    companion object {
        private val log = Logger<PIIEventManager>()
    }
}

enum class PIIProperty(val key: String) {
    PHONE_NUMBER("\$phone_number")
}

fun PropertyOperations.toSecuredEvent(): Event {
    val builder = Event.builder("\$secured_properties")
    for ((operation, properties) in asMap()) {
        builder.property(operation.key, properties)
    }
    return builder.build()
}