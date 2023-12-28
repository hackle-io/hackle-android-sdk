package io.hackle.android.internal.database.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import io.hackle.android.internal.database.shared.NotificationHistoryEntity
import io.hackle.android.internal.database.shared.SharedDatabase
import io.hackle.android.ui.notification.NotificationData
import io.hackle.sdk.core.internal.log.Logger

internal interface NotificationHistoryRepository {
    fun count(workspaceId: Long, environmentId: Long): Int

    fun save(data: NotificationData, timestamp: Long)

    fun getEntities(workspaceId: Long, environmentId: Long, limit: Int? = null): List<NotificationHistoryEntity>

    fun delete(entities: List<NotificationHistoryEntity>)
}

internal class NotificationHistoryRepositoryImpl(
    private val database: SharedDatabase
) : NotificationHistoryRepository {

    override fun count(workspaceId: Long, environmentId: Long): Int {
        return try {
            database.execute(readOnly = true) { db -> count(db, workspaceId, environmentId).toInt() }
        } catch (e: Exception) {
            log.error { "Failed to count notifications: $e" }
            0
        }
    }

    private fun count(db: SQLiteDatabase, workspaceId: Long, environmentId: Long): Long {
        val query =
            "SELECT COUNT(*) FROM ${NotificationHistoryEntity.TABLE_NAME} " +
                "WHERE ${NotificationHistoryEntity.COLUMN_WORKSPACE_ID} = $workspaceId AND " +
                    "${NotificationHistoryEntity.COLUMN_ENVIRONMENT_ID} = $environmentId"
        return db.compileStatement(query)
            .use { statement -> statement.simpleQueryForLong() }
    }

    override fun save(data: NotificationData, timestamp: Long) {
        try {
            database.execute(transaction = true) { db -> save(db, data, timestamp) }
        } catch (e: Exception) {
            log.error { "Failed to save notification: $e" }
        }
    }

    private fun save(db: SQLiteDatabase, data: NotificationData, timestamp: Long) {
        val values = ContentValues()
        values.put(NotificationHistoryEntity.COLUMN_WORKSPACE_ID, data.workspaceId)
        values.put(NotificationHistoryEntity.COLUMN_ENVIRONMENT_ID, data.environmentId)
        values.put(NotificationHistoryEntity.COLUMN_PUSH_MESSAGE_ID, data.pushMessageId)
        values.put(NotificationHistoryEntity.COLUMN_PUSH_MESSAGE_KEY, data.pushMessageKey)
        values.put(NotificationHistoryEntity.COLUMN_PUSH_MESSAGE_EXECUTION_ID, data.pushMessageExecutionId)
        values.put(NotificationHistoryEntity.COLUMN_PUSH_MESSAGE_DELIVERY_ID, data.pushMessageDeliveryId)
        values.put(NotificationHistoryEntity.COLUMN_TIMESTAMP, timestamp)
        values.put(NotificationHistoryEntity.COLUMN_DEBUG, data.debug)
        db.insert(NotificationHistoryEntity.TABLE_NAME, null, values)
    }

    override fun getEntities(workspaceId: Long, environmentId: Long, limit: Int?): List<NotificationHistoryEntity> {
        return try {
            database.execute(readOnly = true) { db ->
                getEntities(db, workspaceId, environmentId, limit)
            }
        } catch (e: Exception) {
            log.error { "Failed to get notifications: $e" }
            emptyList()
        }
    }

    private fun getEntities(
        db: SQLiteDatabase,
        workspaceId: Long,
        environmentId: Long,
        limit: Int? = null
    ): List<NotificationHistoryEntity> {
        var query =
            "SELECT * FROM ${NotificationHistoryEntity.TABLE_NAME} " +
                "WHERE ${NotificationHistoryEntity.COLUMN_WORKSPACE_ID} = $workspaceId AND " +
                    "${NotificationHistoryEntity.COLUMN_ENVIRONMENT_ID} = $environmentId"
        if (limit != null) {
            query += " ORDER BY ${NotificationHistoryEntity.COLUMN_TIMESTAMP} ASC LIMIT $limit"
        }
        return db.rawQuery(query, null).use { cursor ->
            val toReturn = mutableListOf<NotificationHistoryEntity>()
            if (cursor.moveToFirst()) {
                do {
                    val notification = NotificationHistoryEntity.from(cursor)
                    toReturn.add(notification)
                } while (cursor.moveToNext())
            }
            cursor.close()
            return@use toReturn
        }
    }

    override fun delete(entities: List<NotificationHistoryEntity>) {
        try {
            val notificationIds = entities.map { it.historyId }
            database.execute(transaction = true) { db -> delete(db, notificationIds) }
        } catch (e: Exception) {
            log.error { "Failed to delete notification: $e" }
        }
    }

    private fun delete(
        db: SQLiteDatabase,
        notificationIds: List<Long>
    ) {
        val arguments = notificationIds.map { it.toString() }
        val format = "?".repeat(arguments.size)
            .toCharArray()
            .joinToString(",")
        val whereClause = "${NotificationHistoryEntity.COLUMN_HISTORY_ID} IN ($format)"
        db.delete(NotificationHistoryEntity.TABLE_NAME, whereClause, arguments.toTypedArray())
    }

    companion object {

        private val log = Logger<NotificationHistoryRepository>()
    }
}