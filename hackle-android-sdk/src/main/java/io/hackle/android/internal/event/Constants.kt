package io.hackle.android.internal.event

internal object Constants {

    const val USER_EVENT_RETRY_INTERVAL = 60 * 1000 // 1 minute in milliseconds

    const val USER_EVENT_RETRY_MAX_INTERVAL = 10 * 60 * 1000 // 10 minutes in milliseconds

    const val USER_EVENT_EXPIRED_INTERVAL = 7 * 24 * 60 * 60 * 1000 // 7 days in milliseconds
}