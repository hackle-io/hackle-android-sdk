package io.hackle.android.internal.database.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import io.hackle.android.internal.database.shared.NotificationEntity
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.COLUMN_PUSH_MESSAGE_ID
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.COLUMN_CLICK_ACTION
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.COLUMN_CLICK_TIMESTAMP
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.COLUMN_ENVIRONMENT_ID
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.COLUMN_FCM_SENT_TIMESTAMP
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.COLUMN_LINK
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.COLUMN_MESSAGE_ID
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.COLUMN_WORKSPACE_ID
import io.hackle.android.internal.database.shared.NotificationEntity.Companion.TABLE_NAME
import io.hackle.android.internal.database.shared.SharedDatabase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

internal class NotificationRepositoryTest {

    @RelaxedMockK
    private lateinit var database: SharedDatabase

    @MockK
    private lateinit var db: SQLiteDatabase

    private lateinit var sut: NotificationRepositoryImpl

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
        sut = NotificationRepositoryImpl(database)
    }

    @Test
    fun count() {
        val statement = mockk<SQLiteStatement>(relaxUnitFun = true)
        every { statement.simpleQueryForLong() } returns 42
        every { db.compileStatement(any()) } returns statement

        val actual = sut.count()
        expectThat(actual) isEqualTo 42
        verify(exactly = 1) { database.execute(readOnly = true, transaction = false, any()) }
        verify(exactly = 1) { db.compileStatement("SELECT COUNT(*) FROM notifications") }
        verify(exactly = 1) { statement.close() }
    }

    @Test
    fun `should return count 0 when exception occurred`() {
        every { db.compileStatement(any()) } throws IllegalArgumentException()

        val actual = sut.count()
        expectThat(actual) isEqualTo 0
    }

    @Test
    fun save() {
        val notification = mockk<NotificationEntity>(relaxed = true)
        sut.save(notification)

        verify(exactly = 1) { database.execute(readOnly = false, transaction = true, any()) }
        verify(exactly = 1) { db.insert(TABLE_NAME, any(), any()) }
    }

    @Test
    fun `do nothing when exception occurred while saving data`() {
        val notification = mockk<NotificationEntity>(relaxed = true)
        every { db.insert(any(), any(), any()) } throws IllegalArgumentException()

        sut.save(notification)
    }

    @Test
    fun `get notification entities`() {
        val cursor = cursor(
            listOf("0", 123L, 456L, 111L, 1234567890L, "APP_OPEN", 9876543210L, ""),
            listOf("1", 123L, 456L, 222L, 1111111111L, "DEEP_LINK", 3333333333L, "rocketman://main"),
            listOf("2", 123L, 456L, 333L, 2222222222L, "DEEP_LINK", 4444444444L, "rocketman://hidden"),
        )

        every { db.rawQuery(any(), any()) } returns cursor

        val actual = sut.getNotifications(123, 456)

        verify(exactly = 1) { database.execute(readOnly = true, transaction = false, any()) }
        verify(exactly = 1) { db.rawQuery("SELECT * FROM notifications WHERE workspace_id = 123 AND environment_id = 456", null) }
        expectThat(actual) {
            hasSize(3)
            get { this[0] } isEqualTo NotificationEntity("0", 123L, 456L, 111L, 1234567890L, "APP_OPEN", 9876543210L, "")
            get { this[1] } isEqualTo NotificationEntity("1", 123L, 456L, 222L, 1111111111L, "DEEP_LINK", 3333333333L, "rocketman://main")
            get { this[2] } isEqualTo NotificationEntity("2", 123L, 456L, 333L, 2222222222L, "DEEP_LINK", 4444444444L, "rocketman://hidden")
        }
    }

    @Test
    fun delete() {
        val statement = mockk<SQLiteStatement>(relaxUnitFun = true)
        every { db.compileStatement(any()) } returns statement

        val entities = listOf(
            NotificationEntity("0", 123L, 456L, 789L, 0L, "APP_OPEN", 0L, ""),
            NotificationEntity("1", 123L, 456L, 789L, 0L, "APP_OPEN", 0L, ""),
            NotificationEntity("2", 123L, 456L, 789L, 0L, "APP_OPEN", 0L, "")
        )
        sut.delete(entities)

        verify(exactly = 1) { database.execute(readOnly = false, transaction = true, any()) }
        verify(exactly = 1) { db.delete("notifications", "message_id=?", arrayOf("0", "1", "2")) }
    }

    private fun cursor(vararg rows: List<Any>): Cursor {
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

        every { cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID) } answers { 0 }
        every { cursor.getColumnIndexOrThrow(COLUMN_WORKSPACE_ID) } answers { 1 }
        every { cursor.getColumnIndexOrThrow(COLUMN_ENVIRONMENT_ID) } answers { 2 }
        every { cursor.getColumnIndexOrThrow(COLUMN_PUSH_MESSAGE_ID) } answers { 3 }
        every { cursor.getColumnIndexOrThrow(COLUMN_FCM_SENT_TIMESTAMP) } answers { 4 }
        every { cursor.getColumnIndexOrThrow(COLUMN_CLICK_ACTION) } answers { 5 }
        every { cursor.getColumnIndexOrThrow(COLUMN_CLICK_TIMESTAMP) } answers { 6 }
        every { cursor.getColumnIndexOrThrow(COLUMN_LINK) } answers { 7 }

        every { cursor.getLong(any()) } answers { rows[currentIndex][firstArg()] as Long }
        every { cursor.getInt(any()) } answers { rows[currentIndex][firstArg()] as Int }
        every { cursor.getString(any()) } answers { rows[currentIndex][firstArg()] as String }

        return cursor
    }
}