package io.hackle.android.internal.database.shared

import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

internal class NotificationHistoryEntity(
    val historyId: Long,
    val workspaceId: Long,
    val environmentId: Long,
    val pushMessageId: Long?,
    val pushMessageKey: Long?,
    val pushMessageExecutionId: Long?,
    val pushMessageDeliveryId: Long?,
    val journeyId: Long?,
    val journeyKey: Long?,
    val journeyNodeId: Long?,
    val campaignType: String?,
    val timestamp: Long,
    val debug: Boolean
) {

    companion object {

        const val TABLE_NAME = "notification_histories"

        const val COLUMN_HISTORY_ID = "history_id"
        const val COLUMN_WORKSPACE_ID = "workspace_id"
        const val COLUMN_ENVIRONMENT_ID = "environment_id"
        const val COLUMN_PUSH_MESSAGE_ID = "push_message_id"
        const val COLUMN_PUSH_MESSAGE_KEY = "push_message_key"
        const val COLUMN_PUSH_MESSAGE_EXECUTION_ID = "push_message_execution_id"
        const val COLUMN_PUSH_MESSAGE_DELIVERY_ID = "push_message_delivery_id"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_DEBUG = "debug"
        const val COLUMN_JOURNEY_ID = "journey_id"
        const val COLUMN_JOURNEY_KEY = "journey_key"
        const val COLUMN_JOURNEY_NODE_ID = "journey_node_id"
        const val COLUMN_CAMPAIGN_TYPE = "campaign_type"

        const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_WORKSPACE_ID INTEGER NOT NULL," +
                "$COLUMN_ENVIRONMENT_ID INTEGER NOT NULL," +
                "$COLUMN_PUSH_MESSAGE_ID INTEGER," +
                "$COLUMN_PUSH_MESSAGE_KEY INTEGER," +
                "$COLUMN_PUSH_MESSAGE_EXECUTION_ID INTEGER," +
                "$COLUMN_PUSH_MESSAGE_DELIVERY_ID INTEGER," +
                "$COLUMN_TIMESTAMP INTEGER," +
                "$COLUMN_DEBUG INTEGER," +
                "$COLUMN_JOURNEY_ID INTEGER," +
                "$COLUMN_JOURNEY_KEY INTEGER," +
                "$COLUMN_JOURNEY_NODE_ID INTEGER," +
                "$COLUMN_CAMPAIGN_TYPE TEXT" +
            ")"

        const val ADD_JOURNEY_ID =
            "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_JOURNEY_ID INTEGER"

        const val ADD_JOURNEY_KEY =
            "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_JOURNEY_KEY INTEGER"

        const val ADD_JOURNEY_NODE_ID =
            "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_JOURNEY_NODE_ID INTEGER"

        const val ADD_CAMPAIGN_TYPE =
            "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CAMPAIGN_TYPE TEXT"


        fun from(cursor: Cursor): NotificationHistoryEntity {
            return NotificationHistoryEntity(
                historyId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_ID)),
                workspaceId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WORKSPACE_ID)),
                environmentId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENVIRONMENT_ID)),
                pushMessageId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_ID)),
                pushMessageKey = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_KEY)),
                pushMessageExecutionId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_EXECUTION_ID)),
                pushMessageDeliveryId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_DELIVERY_ID)),
                timestamp = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)) ?: 0L,
                debug = cursor.getBoolean(cursor.getColumnIndexOrThrow(COLUMN_DEBUG)),
                journeyId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_JOURNEY_ID)),
                journeyKey = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_JOURNEY_KEY)),
                journeyNodeId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_JOURNEY_NODE_ID)),
                campaignType = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_CAMPAIGN_TYPE))
            )
        }

        private fun Cursor.getBoolean(index: Int): Boolean {
            val value = getIntOrNull(index)
            return value != null && value != 0
        }
    }
}