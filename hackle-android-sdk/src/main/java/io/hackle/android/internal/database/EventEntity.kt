package io.hackle.android.internal.database

internal data class EventEntity(
    val id: Long,
    val status: Status,
    val type: Type,
    val event: String,
) {

    enum class Type(val code: Int) {
        EXPOSURE(0),
        TRACK(1);

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
        const val EVENT_COLUMN_NAME = "event"

        const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME ($ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT, $STATUS_COLUMN_NAME INTEGER, $TYPE_COLUMN_NAME INTEGER, $EVENT_COLUMN_NAME TEXT)"
    }
}
