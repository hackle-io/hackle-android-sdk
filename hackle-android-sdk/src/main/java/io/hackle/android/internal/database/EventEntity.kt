package io.hackle.android.internal.database

import io.hackle.android.internal.event.toDto
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.core.event.UserEvent

internal data class EventEntity(
    val id: Long,
    val status: Status,
    val type: Type,
    val body: String,
) {

    enum class Type(val code: Int) {
        EXPOSURE(0),
        TRACK(1),
        REMOTE_CONFIG(2);

        companion object {
            private val TYPE = values().associateBy { it.code }
            fun from(code: Int): Type {
                return requireNotNull(TYPE[code]) { "code[$code]" }
            }
        }
    }

    enum class Status(val code: Int) {
        PENDING(0),
        FLUSHING(1);

        companion object {
            private val STATUS = values().associateBy { it.code }
            fun from(code: Int): Status {
                return requireNotNull(STATUS[code]) { "code[$code]" }
            }
        }
    }

    companion object {

        const val TABLE_NAME = "events"
        const val ID_COLUMN_NAME = "id"
        const val TYPE_COLUMN_NAME = "type"
        const val STATUS_COLUMN_NAME = "status"
        const val BODY_COLUMN_NAME = "body"

        const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME ($ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT, $STATUS_COLUMN_NAME INTEGER, $TYPE_COLUMN_NAME INTEGER, $BODY_COLUMN_NAME TEXT)"
    }
}

internal val UserEvent.type: EventEntity.Type
    get() {
        return when (this) {
            is UserEvent.Exposure -> EventEntity.Type.EXPOSURE
            is UserEvent.Track -> EventEntity.Type.TRACK
            is UserEvent.RemoteConfig -> EventEntity.Type.REMOTE_CONFIG
        }
    }

internal fun UserEvent.toBody(): String {
    return when (this) {
        is UserEvent.Exposure -> toDto().toJson()
        is UserEvent.Track -> toDto().toJson()
        is UserEvent.RemoteConfig -> toDto().toJson()
    }
}
