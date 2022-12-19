package io.hackle.android.internal.session

import java.util.*

internal data class Session(val id: String) {
    companion object {
        val UNKNOWN = Session("0.ffffffff")
        fun create(timestamp: Long): Session {
            val hash = UUID.randomUUID().toString().take(8)
            return Session("$timestamp.$hash")
        }
    }
}
