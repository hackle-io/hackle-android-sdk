package io.hackle.android.internal.database.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import io.hackle.android.internal.database.shared.NotificationEntity
import io.hackle.android.internal.database.shared.SharedDatabase
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

    fun save(entity: NotificationEntity) {
        try {
            database.execute { db -> save(db, entity) }
        } catch (e: Exception) {
            log.error { "Failed to save event: $e" }
        }
    }

    private fun save(db: SQLiteDatabase, entity: NotificationEntity) {
        val values = ContentValues()
        values.put(NotificationEntity.COLUMN_MESSAGE_ID, entity.messageId)
        values.put(NotificationEntity.COLUMN_WORKSPACE_ID, entity.workspaceId)
        values.put(NotificationEntity.COLUMN_ENVIRONMENT_ID, entity.environmentId)
        values.put(NotificationEntity.COLUMN_CAMPAIGN_ID, entity.campaignId)
        values.put(NotificationEntity.COLUMN_FCM_SENT_TIMESTAMP, entity.fcmSentTimestamp)
        values.put(NotificationEntity.COLUMN_CLICK_ACTION, entity.clickAction)
        values.put(NotificationEntity.COLUMN_LINK, entity.link)
        db.insert(NotificationEntity.TABLE_NAME, null, values)
    }

    fun getNotifications(workspaceId: Long, environmentId: Long, limit: Int): List<NotificationEntity> {
        return try {
            database.execute(transaction = true) { db ->
                getNotifications(db, workspaceId, environmentId, limit)
            }
        } catch (e: Exception) {
            log.error { "Failed to get notifications: $e" }
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
            query += " ORDER BY ${NotificationEntity.COLUMN_FCM_SENT_TIMESTAMP} ASC LIMIT $limit"
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

    fun delete(entities: List<NotificationEntity>) {
        try {
            val messageIds = entities.map { it.messageId }
            database.execute { db -> delete(db, messageIds) }
        } catch (e: Exception) {
            log.error { "Failed to delete notification: $e" }
        }
    }

    private fun delete(
        db: SQLiteDatabase,
        messageIds: List<String>
    ) {
        db.delete(
            NotificationEntity.TABLE_NAME,
            "${NotificationEntity.COLUMN_MESSAGE_ID}=?",
            messageIds.toTypedArray()
        )
    }

    companion object {

        private val log = Logger<NotificationRepository>()
    }
}