package io.hackle.android.internal.database.shared

import android.database.Cursor
import androidx.core.database.getStringOrNull

internal data class NotificationEntity(
    val messageId: String,
    val workspaceId: Long,
    val environmentId: Long,
    val campaignId: Long,
    val fcmSentTime: Long,
    val clickAction: String,
    val link: String?,
) {

    companion object {

        const val TABLE_NAME = "notifications"
        const val COLUMN_MESSAGE_ID = "message_id"
        const val COLUMN_WORKSPACE_ID = "workspace_id"
        const val COLUMN_ENVIRONMENT_ID = "environment_id"
        const val COLUMN_CAMPAIGN_ID = "campaign_id"
        const val COLUMN_FCM_SENT_TIME = "fcm_sent_time"
        const val COLUMN_CLICK_ACTION = "click_action"
        const val COLUMN_LINK = "link"

        const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_MESSAGE_ID TEXT PRIMARY KEY," +
                "$COLUMN_WORKSPACE_ID INTEGER NOT NULL," +
                "$COLUMN_ENVIRONMENT_ID INTEGER NOT NULL," +
                "$COLUMN_CAMPAIGN_ID INTEGER NOT NULL," +
                "$COLUMN_FCM_SENT_TIME INTEGER NOT NULL," +
                "$COLUMN_CLICK_ACTION TEXT," +
                "$COLUMN_LINK TEXT" +
            ")"

        fun from(cursor: Cursor): NotificationEntity {

            return NotificationEntity(
                messageId =  cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)),
                workspaceId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WORKSPACE_ID)),
                environmentId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENVIRONMENT_ID)),
                campaignId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CAMPAIGN_ID)),
                fcmSentTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FCM_SENT_TIME)),
                clickAction = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLICK_ACTION)),
                link = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_LINK))
            )
        }
    }
}