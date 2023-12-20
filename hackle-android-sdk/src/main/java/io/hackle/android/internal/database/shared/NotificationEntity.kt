package io.hackle.android.internal.database.shared

import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

internal data class NotificationEntity(
    val messageId: String,
    val workspaceId: Long,
    val environmentId: Long,
    val pushMessageId: Long?,
    val pushMessageKey: Long?,
    val pushMessageExecutionId: Long?,
    val pushMessageDeliveryId: Long?,
    val clickTimestamp: Long?,
    val debug: Boolean
) {

    companion object {

        const val TABLE_NAME = "notifications"

        const val COLUMN_MESSAGE_ID = "message_id"
        const val COLUMN_WORKSPACE_ID = "workspace_id"
        const val COLUMN_ENVIRONMENT_ID = "environment_id"
        const val COLUMN_PUSH_MESSAGE_ID = "push_message_id"
        const val COLUMN_PUSH_MESSAGE_KEY = "push_message_key"
        const val COLUMN_PUSH_MESSAGE_EXECUTION_ID = "push_message_execution_id"
        const val COLUMN_PUSH_MESSAGE_DELIVERY_ID = "push_message_delivery_id"
        const val COLUMN_CLICK_TIMESTAMP = "click_timestamp"
        const val COLUMN_DEBUG = "debug"

        const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_MESSAGE_ID TEXT PRIMARY KEY," +
                "$COLUMN_WORKSPACE_ID INTEGER NOT NULL," +
                "$COLUMN_ENVIRONMENT_ID INTEGER NOT NULL," +
                "$COLUMN_PUSH_MESSAGE_ID INTEGER," +
                "$COLUMN_PUSH_MESSAGE_KEY INTEGER," +
                "$COLUMN_PUSH_MESSAGE_EXECUTION_ID INTEGER," +
                "$COLUMN_PUSH_MESSAGE_DELIVERY_ID INTEGER," +
                "$COLUMN_CLICK_TIMESTAMP INTEGER," +
                "$COLUMN_DEBUG INTEGER" +
            ")"

        fun from(cursor: Cursor): NotificationEntity {
            return NotificationEntity(
                messageId =  cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)),
                workspaceId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WORKSPACE_ID)),
                environmentId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENVIRONMENT_ID)),
                pushMessageId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_ID)),
                pushMessageKey = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_KEY)),
                pushMessageExecutionId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_EXECUTION_ID)),
                pushMessageDeliveryId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_DELIVERY_ID)),
                clickTimestamp = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_CLICK_TIMESTAMP)),
                debug = cursor.getBoolean(cursor.getColumnIndexOrThrow(COLUMN_DEBUG))
            )
        }

        private fun Cursor.getBoolean(index: Int): Boolean {
            val value = getIntOrNull(index)
            return value != null && value != 0
        }
    }
}