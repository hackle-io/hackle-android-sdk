package io.hackle.android.internal.database.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import io.hackle.android.internal.database.shared.NotificationEntity
import io.hackle.android.internal.database.shared.SharedDatabase
import io.hackle.sdk.core.internal.log.Logger

internal interface NotificationRepository {
    fun count(): Int

    fun save(entity: NotificationEntity)

    fun getNotifications(workspaceId: Long, environmentId: Long, limit: Int? = null): List<NotificationEntity>

    fun delete(entities: List<NotificationEntity>)
}

internal class NotificationRepositoryImpl(
    private val database: SharedDatabase
) : NotificationRepository {

    override fun count(): Int {
        return try {
            database.execute(readOnly = true) { db -> count(db).toInt() }
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

    override fun save(entity: NotificationEntity) {
        try {
            database.execute(transaction = true) { db -> save(db, entity) }
        } catch (e: Exception) {
            log.error { "Failed to save event: $e" }
        }
    }

    private fun save(db: SQLiteDatabase, entity: NotificationEntity) {
        val values = ContentValues()
        values.put(NotificationEntity.COLUMN_MESSAGE_ID, entity.messageId)
        values.put(NotificationEntity.COLUMN_WORKSPACE_ID, entity.workspaceId)
        values.put(NotificationEntity.COLUMN_ENVIRONMENT_ID, entity.environmentId)
        values.put(NotificationEntity.COLUMN_PUSH_MESSAGE_ID, entity.pushMessageId)
        values.put(NotificationEntity.COLUMN_FCM_SENT_TIMESTAMP, entity.fcmSentTimestamp)
        values.put(NotificationEntity.COLUMN_CLICK_ACTION, entity.clickAction)
        values.put(NotificationEntity.COLUMN_LINK, entity.link)
        db.insert(NotificationEntity.TABLE_NAME, null, values)
    }

    override fun getNotifications(workspaceId: Long, environmentId: Long, limit: Int?): List<NotificationEntity> {
        return try {
            database.execute(readOnly = true) { db ->
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

    override fun delete(entities: List<NotificationEntity>) {
        try {
            val messageIds = entities.map { it.messageId }
            database.execute(transaction = true) { db -> delete(db, messageIds) }
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