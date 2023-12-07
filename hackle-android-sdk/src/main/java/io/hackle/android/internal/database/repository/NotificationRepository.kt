package io.hackle.android.internal.database.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import io.hackle.android.internal.database.shared.NotificationEntity
import io.hackle.android.internal.database.shared.SharedDatabase
import io.hackle.android.ui.notification.NotificationData
import io.hackle.sdk.core.internal.log.Logger

internal class NotificationRepository(
    private val database: SharedDatabase
) {

    fun count(): Long {
        return try {
            database.execute(readOnly = true) { db -> count(db) }
        } catch (e: Exception) {
            log.error { "Failed to count push messages: $e" }
            0
        }
    }

    private fun count(db: SQLiteDatabase): Long {
        val query = "SELECT COUNT(*) FROM ${NotificationEntity.TABLE_NAME}"
        return db.compileStatement(query)
            .use { statement -> statement.simpleQueryForLong() }
    }

    fun save(data: NotificationData) {
        try {
            database.execute { db -> save(db, data) }
        } catch (e: Exception) {
            log.error(e) { "Failed to save event" }
        }
    }

    private fun save(db: SQLiteDatabase, data: NotificationData) {
        val values = ContentValues()
        values.put(NotificationEntity.COLUMN_MESSAGE_ID, data.messageId)
        values.put(NotificationEntity.COLUMN_WORKSPACE_ID, data.workspaceId)
        values.put(NotificationEntity.COLUMN_ENVIRONMENT_ID, data.environmentId)
        values.put(NotificationEntity.COLUMN_CAMPAIGN_ID, data.campaignId)
        values.put(NotificationEntity.COLUMN_FCM_SENT_TIME, data.fcmSentTime)
        values.put(NotificationEntity.COLUMN_CLICK_ACTION, data.clickAction.text)
        values.put(NotificationEntity.COLUMN_LINK, data.link)
        db.insert(NotificationEntity.TABLE_NAME, null, values)
    }

    fun getNotifications(workspaceId: Long, environmentId: Long, limit: Int): List<NotificationEntity> {
        return try {
            database.execute(transaction = true) { db ->
                getNotifications(db, workspaceId, environmentId, limit)
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to get notifications" }
            emptyList()
        }
    }

    private fun getNotifications(
        db: SQLiteDatabase,
        workspaceId: Long,
        environmentId: Long,
        limit: Int? = null
    ): List<NotificationEntity> {
        var query =
            "SELECT * FROM ${NotificationEntity.TABLE_NAME} " +
                "WHERE ${NotificationEntity.COLUMN_WORKSPACE_ID} = $workspaceId AND " +
                    "${NotificationEntity.COLUMN_ENVIRONMENT_ID} = $environmentId"
        if (limit != null) {
            query += " ORDER BY ${NotificationEntity.COLUMN_FCM_SENT_TIME} ASC LIMIT $limit"
        }
        return db.rawQuery(query, null).use { cursor ->
            val toReturn = mutableListOf<NotificationEntity>()
            if (cursor.moveToFirst()) {
                do {
                    val notification = NotificationEntity.from(cursor)
                    toReturn.add(notification)
                } while (cursor.moveToNext())
            }
            cursor.close()
            return@use toReturn
        }
    }

    companion object {

        private val log = Logger<NotificationRepository>()
    }
}