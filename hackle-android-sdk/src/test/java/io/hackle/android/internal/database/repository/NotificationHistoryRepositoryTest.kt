package io.hackle.android.internal.database.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import io.hackle.android.internal.database.shared.NotificationHistoryEntity
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_DEBUG
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_ENVIRONMENT_ID
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_HISTORY_ID
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_PUSH_MESSAGE_DELIVERY_ID
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_PUSH_MESSAGE_EXECUTION_ID
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_PUSH_MESSAGE_ID
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_PUSH_MESSAGE_KEY
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_TIMESTAMP
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.COLUMN_WORKSPACE_ID
import io.hackle.android.internal.database.shared.NotificationHistoryEntity.Companion.TABLE_NAME
import io.hackle.android.internal.database.shared.SharedDatabase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

internal class NotificationHistoryRepositoryTest {

    @RelaxedMockK
    private lateinit var database: SharedDatabase

    @MockK
    private lateinit var db: SQLiteDatabase

    private lateinit var sut: NotificationHistoryRepositoryImpl

    @Before
    fun before() {
        MockKAnnotations.init(this)

        every {
            database.execute(
                readOnly = any(),
                transaction = any(),
                command = any<(SQLiteDatabase) -> Any>()
            )
        } answers {
            thirdArg<(SQLiteDatabase) -> Any>().invoke(db)
        }
        sut = NotificationHistoryRepositoryImpl(database)
    }

    @Test
    fun count() {
        val statement = mockk<SQLiteStatement>(relaxUnitFun = true)
        every { statement.simpleQueryForLong() } returns 42
        every { db.compileStatement(any()) } returns statement

        val actual = sut.count(1, 2)
        expectThat(actual) isEqualTo 42
        verify(exactly = 1) { database.execute(readOnly = true, transaction = false, any()) }
        verify(exactly = 1) { db.compileStatement("SELECT COUNT(*) FROM notification_histories WHERE workspace_id = 1 AND environment_id = 2") }
        verify(exactly = 1) { statement.close() }
    }

    @Test
    fun `should return count 0 when exception occurred`() {
        every { db.compileStatement(any()) } throws IllegalArgumentException()

        val actual = sut.count(1, 2)
        expectThat(actual) isEqualTo 0
    }

    @Test
    fun save() {
        val notification = mockk<NotificationHistoryEntity>(relaxed = true)
        sut.save(notification)

        verify(exactly = 1) { database.execute(readOnly = false, transaction = true, any()) }
        verify(exactly = 1) { db.insert(TABLE_NAME, any(), any()) }
    }

    @Test
    fun `do nothing when exception occurred while saving data`() {
        val notification = mockk<NotificationHistoryEntity>(relaxed = true)
        every { db.insert(any(), any(), any()) } throws IllegalArgumentException()

        sut.save(notification)
    }

    @Test
    fun `get notification entities`() {
        val cursor = cursor(
            listOf(0L, 1L, 2L, 3L, 4L, 5L, 6L, 1234567890L, 1),
            listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 3333333333L, 0),
            listOf(2L, 3L, 4L, 5L, 6L, 7L, 8L, 4444444444L, 11),
        )
        every { db.rawQuery(any(), any()) } returns cursor

        val actual = sut.getEntities(123, 456)

        verify(exactly = 1) { database.execute(readOnly = true, transaction = false, any()) }
        verify(exactly = 1) { db.rawQuery("SELECT * FROM notification_histories WHERE workspace_id = 123 AND environment_id = 456", null) }
        expectThat(actual) {
            hasSize(3)
            get { this[0] } isEqualTo NotificationHistoryEntity(0L, 1L, 2L, 3L, 4L, 5L, 6L, 1234567890L, true)
            get { this[1] } isEqualTo NotificationHistoryEntity(1L, 2L, 3L, 4L, 5L, 6L, 7L, 3333333333L, false)
            get { this[2] } isEqualTo NotificationHistoryEntity(2L, 3L, 4L, 5L, 6L, 7L, 8L, 4444444444L, true)
        }
    }

    @Test
    fun `delete multiple rows`() {
        val statement = mockk<SQLiteStatement>(relaxUnitFun = true)
        every { db.compileStatement(any()) } returns statement

        val entities = listOf(
            NotificationHistoryEntity(0, 123L, 456L, 789L, 111L, 222L, 333L, 444L, true),
            NotificationHistoryEntity(1, 123L, 456L, 789L, 111L, 222L, 333L, 444L, true),
            NotificationHistoryEntity(2, 123L, 456L, 789L, 111L, 222L, 333L, 444L, true),
        )
        sut.delete(entities)

        verify(exactly = 1) { database.execute(readOnly = false, transaction = true, any()) }
        verify(exactly = 1) { db.delete("notification_histories", "history_id IN (?,?,?)", arrayOf("0", "1", "2")) }
    }

    private fun cursor(vararg rows: List<Any>): Cursor =
        cursor(
            listOf(
                COLUMN_HISTORY_ID,
                COLUMN_WORKSPACE_ID,
                COLUMN_ENVIRONMENT_ID,
                COLUMN_PUSH_MESSAGE_ID,
                COLUMN_PUSH_MESSAGE_KEY,
                COLUMN_PUSH_MESSAGE_EXECUTION_ID,
                COLUMN_PUSH_MESSAGE_DELIVERY_ID,
                COLUMN_TIMESTAMP,
                COLUMN_DEBUG
            ),
            *rows
        )

    private fun cursor(columnNames: List<String>, vararg rows: List<Any>): Cursor {
        var currentIndex = -1
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } answers {
            currentIndex = 0
            true
        }
        every { cursor.moveToNext() } answers {
            currentIndex++
            currentIndex < rows.size
        }

        for ((index, name) in columnNames.withIndex()) {
            every { cursor.getColumnIndex(name) } answers { index }
            every { cursor.getColumnIndexOrThrow(name) } answers { index }
        }

        every { cursor.getLong(any()) } answers { (rows[currentIndex][firstArg()] as Number).toLong() }
        every { cursor.getInt(any()) } answers { (rows[currentIndex][firstArg()] as Number).toInt() }
        every { cursor.getString(any()) } answers { rows[currentIndex][firstArg()] as String }

        return cursor
    }

    infix fun Assertion.Builder<NotificationHistoryEntity>.isEqualTo(expected: NotificationHistoryEntity): Assertion.Builder<NotificationHistoryEntity> =
        assert("is equal to %s", expected) {
            if (it.historyId == expected.historyId &&
                it.workspaceId == expected.workspaceId &&
                it.environmentId == expected.environmentId &&
                it.pushMessageId == expected.pushMessageId &&
                it.pushMessageKey == expected.pushMessageKey &&
                it.pushMessageExecutionId == expected.pushMessageExecutionId &&
                it.pushMessageDeliveryId == expected.pushMessageDeliveryId &&
                it.timestamp == expected.timestamp &&
                it.debug == expected.debug) {
                pass(actual = it)
            } else {
                fail(actual = it)
            }
        }
}