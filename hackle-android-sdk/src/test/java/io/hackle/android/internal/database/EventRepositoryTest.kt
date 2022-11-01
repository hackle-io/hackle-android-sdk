package io.hackle.android.internal.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import io.hackle.android.internal.database.EventEntity.Companion.TABLE_NAME
import io.hackle.android.internal.database.EventEntity.Status.FLUSHING
import io.hackle.android.internal.database.EventEntity.Status.PENDING
import io.hackle.android.internal.database.EventEntity.Type.EXPOSURE
import io.hackle.android.internal.database.EventEntity.Type.TRACK
import io.hackle.sdk.core.event.UserEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

internal class EventRepositoryTest {

    @RelaxedMockK
    private lateinit var databaseHelper: DatabaseHelper

    @MockK
    private lateinit var db: SQLiteDatabase

    private lateinit var sut: EventRepository

    @Before
    fun before() {
        MockKAnnotations.init(this)

        every {
            databaseHelper.execute(
                readOnly = any(),
                transaction = any(),
                command = any<(SQLiteDatabase) -> Any>())
        } answers {
            thirdArg<(SQLiteDatabase) -> Any>().invoke(db)
        }
        sut = EventRepository(databaseHelper)
    }

    @Test
    fun `count - 전체 이벤트 카운트`() {
        // given
        val statement = mockk<SQLiteStatement>(relaxUnitFun = true)
        every { statement.simpleQueryForLong() } returns 42
        every { db.compileStatement(any()) } returns statement

        // when
        val actual = sut.count()

        // then
        expectThat(actual) isEqualTo 42
        verify(exactly = 1) { databaseHelper.execute(readOnly = true, transaction = false, any()) }
        verify(exactly = 1) { db.compileStatement("SELECT COUNT(*) FROM events") }
        verify(exactly = 1) { statement.close() }
    }

    @Test
    fun `count - 특정 상태 카운트`() {
        // given
        val statement = mockk<SQLiteStatement>(relaxUnitFun = true)
        every { statement.simpleQueryForLong() } returns 42
        every { db.compileStatement(any()) } returns statement

        // when
        val actual = sut.count(PENDING)

        // then
        expectThat(actual) isEqualTo 42
        verify(exactly = 1) { databaseHelper.execute(readOnly = true, transaction = false, any()) }
        verify(exactly = 1) { db.compileStatement("SELECT COUNT(*) FROM events WHERE status = 0") }
        verify(exactly = 1) { statement.close() }
    }

    @Test
    fun `count - 예외가 발생하면 0 리턴`() {
        // given
        every { db.compileStatement(any()) } throws IllegalArgumentException()

        // when
        val actual = sut.count()

        // then
        expectThat(actual) isEqualTo 0
    }

    @Test
    fun `save - UserEvent 를 저장한다`() {
        // given
        val event = mockk<UserEvent.Track>(relaxed = true)

        // when
        sut.save(event)

        // then
        verify(exactly = 1) { databaseHelper.execute(readOnly = false, transaction = false, any()) }
        verify(exactly = 1) { db.insert(TABLE_NAME, any(), any()) }
    }

    @Test
    fun `save - UserEvent 를 저장하다 실패해도 무시한다`() {
        every { db.insert(any(), any(), any()) } throws IllegalArgumentException()
        val event = mockk<UserEvent.Track>(relaxed = true)

        try {
            sut.save(event)
        } catch (e: Exception) {
            fail()
        }
    }

    @Test
    fun `getEventsToFlush - PENDING 이벤트를 가져오고 FLUSHING 상태로 바꾼다`() {
        // given
        val cursor = cursor(
            listOf(1L, 0, 0, "body1"),
            listOf(2L, 0, 0, "body2"),
            listOf(3L, 0, 1, "body3"),
            listOf(4L, 0, 1, "body4"),
        )

        every { db.rawQuery(any(), any()) } returns cursor

        val statement = mockk<SQLiteStatement>(relaxed = true)
        every { db.compileStatement(any()) } returns statement

        // when
        val actual = sut.getEventsToFlush(10)

        // then
        expectThat(actual) {
            hasSize(4)
            get { this[0] } isEqualTo EventEntity(1L, PENDING, EXPOSURE, "body1")
            get { this[1] } isEqualTo EventEntity(2L, PENDING, EXPOSURE, "body2")
            get { this[2] } isEqualTo EventEntity(3L, PENDING, TRACK, "body3")
            get { this[3] } isEqualTo EventEntity(4L, PENDING, TRACK, "body4")
        }

        verify(exactly = 1) { databaseHelper.execute(readOnly = false, transaction = true, any()) }
        verify(exactly = 4) { statement.executeUpdateDelete() }
        verify(exactly = 1) { db.compileStatement("UPDATE events SET status = 1 WHERE id = 1") }
        verify(exactly = 1) { db.compileStatement("UPDATE events SET status = 1 WHERE id = 2") }
        verify(exactly = 1) { db.compileStatement("UPDATE events SET status = 1 WHERE id = 3") }
        verify(exactly = 1) { db.compileStatement("UPDATE events SET status = 1 WHERE id = 4") }
    }

    private fun cursor(vararg rows: List<Any>): Cursor {

        var currentIndex = -1
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToNext() } answers {
            currentIndex++
            currentIndex < rows.size
        }

        every { cursor.getLong(any()) } answers { rows[currentIndex][firstArg()] as Long }
        every { cursor.getInt(any()) } answers { rows[currentIndex][firstArg()] as Int }
        every { cursor.getString(any()) } answers { rows[currentIndex][firstArg()] as String }

        return cursor
    }

    @Test
    fun `getEventsToFlush - 이벤트를 가져오다 예외가 발생하면 빈 리스트를 리턴한다`() {
        // given
        every { db.rawQuery(any(), any()) } throws IllegalArgumentException()

        // when
        val actual = sut.getEventsToFlush(10)

        // then
        expectThat(actual).hasSize(0)
    }

    @Test
    fun `findAllBy - 입력한 status 의 이벤트를 가져온다`() {
        // given
        val cursor = cursor(
            listOf(1L, 1, 0, "body1"),
            listOf(2L, 1, 0, "body2"),
            listOf(3L, 1, 1, "body3"),
            listOf(4L, 1, 1, "body4"),
        )

        every { db.rawQuery(any(), any()) } returns cursor

        // when
        val actual = sut.findAllBy(FLUSHING)

        // then
        verify(exactly = 1) { databaseHelper.execute(readOnly = true, transaction = false, any()) }
        verify(exactly = 1) { db.rawQuery("SELECT * FROM events WHERE status = 1", null) }
        expectThat(actual) {
            hasSize(4)
            get { this[0] } isEqualTo EventEntity(1L, FLUSHING, EXPOSURE, "body1")
            get { this[1] } isEqualTo EventEntity(2L, FLUSHING, EXPOSURE, "body2")
            get { this[2] } isEqualTo EventEntity(3L, FLUSHING, TRACK, "body3")
            get { this[3] } isEqualTo EventEntity(4L, FLUSHING, TRACK, "body4")
        }
    }

    @Test
    fun `findAllBy - 이벤트를 가져오다 예외가 발생하면 빈 리스트를 리턴한다`() {
        // given
        every { db.rawQuery(any(), any()) } throws IllegalArgumentException()

        // when
        val actual = sut.findAllBy(FLUSHING)

        // then
        expectThat(actual).hasSize(0)
    }

    @Test
    fun `update - 입력받은 상태로 업데이트한다`() {
        // given
        val events = listOf(
            EventEntity(1L, PENDING, EXPOSURE, "body"),
            EventEntity(2L, PENDING, EXPOSURE, "body"),
            EventEntity(3L, PENDING, TRACK, "body"),
            EventEntity(4L, PENDING, TRACK, "body"),
        )
        val statement = mockk<SQLiteStatement>(relaxed = true)
        every { db.compileStatement(any()) } returns statement

        // when
        sut.update(events, FLUSHING)

        // then
        verify(exactly = 1) { databaseHelper.execute(readOnly = false, transaction = true, any()) }
        verify(exactly = 4) { statement.executeUpdateDelete() }
        verify(exactly = 1) { db.compileStatement("UPDATE events SET status = 1 WHERE id = 1") }
        verify(exactly = 1) { db.compileStatement("UPDATE events SET status = 1 WHERE id = 2") }
        verify(exactly = 1) { db.compileStatement("UPDATE events SET status = 1 WHERE id = 3") }
        verify(exactly = 1) { db.compileStatement("UPDATE events SET status = 1 WHERE id = 4") }
    }

    @Test
    fun `update - 업데이트하다 실패해도 무시한다`() {
        // given
        val events = listOf(
            EventEntity(1L, PENDING, EXPOSURE, "body"),
            EventEntity(2L, PENDING, EXPOSURE, "body"),
            EventEntity(3L, PENDING, TRACK, "body"),
            EventEntity(4L, PENDING, TRACK, "body"),
        )
        every { db.compileStatement(any()) } throws IllegalArgumentException()

        // when
        try {
            sut.update(events, FLUSHING)
        } catch (e: Exception) {
            fail()
        }
    }

    @Test
    fun `deleteOldEvents - 입력받은 count 만큼 오래된 이벤트를 삭제한다`() {
        // given
        val statement = mockk<SQLiteStatement>(relaxUnitFun = true)
        every { statement.simpleQueryForLong() } returns 42
        every { db.compileStatement(any()) } returns statement

        // when
        sut.deleteOldEvents(12)

        // then
        verify(exactly = 1) { databaseHelper.execute(readOnly = false, transaction = true, any()) }
        verify(exactly = 1) { db.compileStatement("SELECT id FROM events LIMIT 1 OFFSET 11") }
        verify(exactly = 1) { db.delete("events", "id <= 42", null) }
    }

    @Test
    fun `deleteOldEvents - 삭제하다 예외가 발생해도 무시한다`() {
        // given
        every { db.compileStatement(any()) } throws IllegalArgumentException()

        // when
        try {
            sut.deleteOldEvents(42)
        } catch (e: Exception) {
            fail()
        }
    }
}