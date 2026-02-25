package io.hackle.sdk.common

/**
 * Defines the conditions that trigger session expiration when user identifiers change.
 */
enum class HackleSessionExpiry {

    /**
     * Session expires when the userId changes from one value to another (e.g. "A" -> "B").
     */
    USER_ID_CHANGE,

    /**
     * Session expires when the deviceId changes.
     */
    DEVICE_ID_CHANGE,

    /**
     * Session expires when the userId changes from null to a non-null value (e.g. null -> "A").
     */
    NULL_TO_USER_ID,

    /**
     * Session expires when the userId changes from a non-null value to null (e.g. "A" -> null).
     */
    USER_ID_TO_NULL
}
