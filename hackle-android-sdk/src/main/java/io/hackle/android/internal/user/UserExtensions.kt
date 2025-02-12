package io.hackle.android.internal.user

import io.hackle.android.internal.model.Device
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.Cohort
import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.model.TargetEvent
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType

internal val HackleUser.hackleDeviceId: String? get() = identifiers[IdentifierType.HACKLE_DEVICE_ID.key]

internal fun User.identifierEquals(other: User?): Boolean {
    if (other == null) {
        return false
    }
    return this.userId == other.userId && this.deviceId == other.deviceId
}

internal fun User.mergeWith(other: User?): User {
    return when {
        other == null -> this
        this.identifierEquals(other) -> copy(properties = other.properties + this.properties)
        else -> this
    }
}

internal fun User.with(device: Device): User {
    val builder = toBuilder()
    if (id == null) {
        builder.id(device.id)
    }
    if (deviceId == null) {
        builder.deviceId(device.id)
    }
    return builder.build()
}

internal val User.resolvedIdentifiers: Identifiers get() = Identifiers.from(this)

internal fun UserCohorts.filterBy(user: User): UserCohorts {
    val identifiers = user.resolvedIdentifiers
    val filtered = asMap().filter { it.key in identifiers }
    return UserCohorts.from(filtered)
}

internal fun UserCohorts.rawCohorts(): List<Cohort> {
    return asList().flatMap { it.cohorts }
}

internal fun UserTargetEvents.rawEvents(): List<TargetEvent> {
    return asList()
}