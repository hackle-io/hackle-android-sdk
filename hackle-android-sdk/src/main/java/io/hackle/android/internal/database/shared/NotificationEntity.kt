package io.hackle.android.internal.database.shared

import android.database.Cursor
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

internal data class NotificationEntity(
    val messageId: String,
    val workspaceId: Long,
    val environmentId: Long,
    val pushMessageId: Long,
    val fcmSentTimestamp: Long,
    val clickAction: String,
    val clickTimestamp: Long?,
    val link: String?,
) {

    companion object {

        const val TABLE_NAME = "notifications"
        const val COLUMN_MESSAGE_ID = "message_id"
        const val COLUMN_WORKSPACE_ID = "workspace_id"
        const val COLUMN_ENVIRONMENT_ID = "environment_id"
        const val COLUMN_PUSH_MESSAGE_ID = "push_message_id"
        const val COLUMN_FCM_SENT_TIMESTAMP = "fcm_sent_timestamp"
        const val COLUMN_CLICK_ACTION = "click_action"
        const val COLUMN_CLICK_TIMESTAMP = "click_timestamp"
        const val COLUMN_LINK = "link"

        const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_MESSAGE_ID TEXT PRIMARY KEY," +
                "$COLUMN_WORKSPACE_ID INTEGER NOT NULL," +
                "$COLUMN_ENVIRONMENT_ID INTEGER NOT NULL," +
                "$COLUMN_PUSH_MESSAGE_ID INTEGER NOT NULL," +
                "$COLUMN_FCM_SENT_TIMESTAMP INTEGER NOT NULL," +
                "$COLUMN_CLICK_ACTION TEXT," +
                "$COLUMN_CLICK_TIMESTAMP INTEGER," +
                "$COLUMN_LINK TEXT" +
            ")"

        fun from(cursor: Cursor): NotificationEntity {
            return NotificationEntity(
                messageId =  cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)),
                workspaceId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WORKSPACE_ID)),
                environmentId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENVIRONMENT_ID)),
                pushMessageId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_ID)),
                fcmSentTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FCM_SENT_TIMESTAMP)),
                clickAction = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLICK_ACTION)),
                clickTimestamp = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_CLICK_TIMESTAMP)),
                link = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_LINK))
            )
        }
    }
}