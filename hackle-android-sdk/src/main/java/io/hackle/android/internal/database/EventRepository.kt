package io.hackle.android.internal.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.hackle.android.internal.database.EventEntity.Companion.BODY_COLUMN_NAME
import io.hackle.android.internal.database.EventEntity.Companion.ID_COLUMN_NAME
import io.hackle.android.internal.database.EventEntity.Companion.STATUS_COLUMN_NAME
import io.hackle.android.internal.database.EventEntity.Companion.TABLE_NAME
import io.hackle.android.internal.database.EventEntity.Companion.TYPE_COLUMN_NAME
import io.hackle.android.internal.database.EventEntity.Status.FLUSHING
import io.hackle.android.internal.database.EventEntity.Status.PENDING
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger

internal class EventRepository(
    private val databaseHelper: DatabaseHelper,
) {

    fun count(status: EventEntity.Status? = null): Long {
        return try {
            databaseHelper.execute(readOnly = true) { db -> count(db, status) }
        } catch (e: Exception) {
            log.error { "Failed to count events: $e" }
            0
        }
    }

    private fun count(db: SQLiteDatabase, status: EventEntity.Status? = null): Long {
        val query = if (status != null) {
            "SELECT COUNT(*) FROM $TABLE_NAME WHERE $STATUS_COLUMN_NAME = ${status.code}"
        } else {
            "SELECT COUNT(*) FROM $TABLE_NAME"
        }
        return db.compileStatement(query)
            .use { statement -> statement.simpleQueryForLong() }
    }

    fun save(userEvent: UserEvent) {
        try {
            databaseHelper.execute { db -> save(db, userEvent) }
        } catch (e: Exception) {
            log.error { "Failed to save event: $e" }
        }
    }

    private fun save(db: SQLiteDatabase, userEvent: UserEvent) {
        val values = ContentValues()
        values.put(TYPE_COLUMN_NAME, userEvent.type.code)
        values.put(STATUS_COLUMN_NAME, PENDING.code)
        values.put(BODY_COLUMN_NAME, userEvent.toBody())
        db.insert(TABLE_NAME, null, values)
    }

    fun getEventsToFlush(limit: Int): List<EventEntity> {
        return try {
            databaseHelper.execute(transaction = true) { db -> getEventsToFlush(db, limit) }
        } catch (e: Exception) {
            log.error { "Failed to get events: $e" }
            emptyList()
        }
    }

    private fun getEventsToFlush(db: SQLiteDatabase, limit: Int): List<EventEntity> {
        return getEvents(db, PENDING, limit)
            .also { update(db, it, FLUSHING) }
    }

    private fun getEvents(
        db: SQLiteDatabase,
        status: EventEntity.Status,
        limit: Int? = null,
    ): List<EventEntity> {
        val query = if (limit != null) {
            "SELECT * FROM $TABLE_NAME WHERE $STATUS_COLUMN_NAME = ${status.code} ORDER BY $ID_COLUMN_NAME ASC LIMIT $limit"
        } else {
            "SELECT * FROM $TABLE_NAME WHERE $STATUS_COLUMN_NAME = ${status.code}"
        }

        return db.rawQuery(query, null).use { cursor ->
            val events = mutableListOf<EventEntity>()
            while (cursor.moveToNext()) {
                val event = eventEntity(cursor)
                events.add(event)
            }
            events
        }
    }

    private fun eventEntity(cursor: Cursor): EventEntity {
        return EventEntity(
            id = cursor.getLong(0),
            status = EventEntity.Status.from(cursor.getInt(1)),
            type = EventEntity.Type.from(cursor.getInt(2)),
            body = cursor.getString(3)
        )
    }

    fun findAllBy(status: EventEntity.Status): List<EventEntity> {
        return try {
            databaseHelper.execute(readOnly = true) { db -> getEvents(db, status) }
        } catch (e: Exception) {
            log.error { "Failed to get events: $e" }
            emptyList()
        }
    }

    fun update(events: List<EventEntity>, status: EventEntity.Status) {
        try {
            databaseHelper.execute(transaction = true) { db -> update(db, events, status) }
        } catch (e: Exception) {
            log.error { "Failed to update events: $e" }
        }
    }

    private fun update(db: SQLiteDatabase, events: List<EventEntity>, status: EventEntity.Status) {
        for (event in events) {
            val query =
                "UPDATE $TABLE_NAME SET $STATUS_COLUMN_NAME = ${status.code} WHERE $ID_COLUMN_NAME = ${event.id}"
            db.compileStatement(query)
                .use { statement -> statement.executeUpdateDelete() }
        }
    }

    fun delete(events: List<EventEntity>) {
        val ids = events.joinToString(separator = ",") { it.id.toString() }
        val whereClause = "$ID_COLUMN_NAME IN ($ids)"
        try {
            databaseHelper.execute { db -> db.delete(TABLE_NAME, whereClause, null) }
        } catch (e: Exception) {
            log.error { "Failed to delete events: $e" }
        }
    }

    fun deleteOldEvents(count: Int) {
        return try {
            databaseHelper.execute(transaction = true) { db -> deleteOldEvents(db, count) }
        } catch (e: Exception) {
            log.error { "Failed to delete events: $e" }
        }
    }

    private fun deleteOldEvents(db: SQLiteDatabase, count: Int) {
        val query = "SELECT $ID_COLUMN_NAME FROM $TABLE_NAME LIMIT 1 OFFSET ${count - 1}"
        val id = db.compileStatement(query)
            .use { statement -> statement.simpleQueryForLong() }
        db.delete(TABLE_NAME, "$ID_COLUMN_NAME <= $id", null)
    }

    companion object {
        private val log = Logger<EventRepository>()
    }
}
