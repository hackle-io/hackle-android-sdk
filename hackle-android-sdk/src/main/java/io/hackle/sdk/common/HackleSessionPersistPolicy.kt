package io.hackle.sdk.common

/**
 * Defines the conditions under which a session is persisted when user identifiers change.
 */
enum class HackleSessionPersistPolicy {

    /**
     * Persist session when the userId changes from one value to another (e.g. "A" -> "B").
     */
    USER_ID_CHANGE,

    /**
     * Persist session when the deviceId changes. (e.g. "C" -> "D")
     */
    DEVICE_ID_CHANGE,

    /**
     * Persist session when the userId changes from null to a non-null value (e.g. null -> "A").
     */
    NULL_TO_USER_ID,

    /**
     * Persist session when the userId changes from a non-null value to null (e.g. "A" -> null).
     */
    USER_ID_TO_NULL
}
