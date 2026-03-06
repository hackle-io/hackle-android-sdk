package io.hackle.sdk.common

/**
 * Defines a condition under which the current session is preserved when user identifiers change.
 */
fun interface HackleSessionPersistCondition {

    /**
     * Determines whether the current session should be preserved for the given user change.
     *
     * @param oldUser the user before the change
     * @param newUser the user after the change
     * @return `true` to keep the current session, `false` to start a new session
     */
    fun shouldPersist(oldUser: User, newUser: User): Boolean

    companion object {

        /**
         * Preserves the session when userId changes from `null` to a non-null value (e.g. `null` → `"A"`).
         *
         * If other identifier changes also occur (e.g. deviceId changes simultaneously),
         * the session is still preserved as long as this condition is met.
         */
        @JvmField
        val NULL_TO_USER_ID: HackleSessionPersistCondition = HackleSessionPersistCondition { oldUser, newUser ->
            oldUser.userId == null && newUser.userId != null
        }
    }
}
