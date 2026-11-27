package io.hackle.android.internal.database.shared

import android.database.sqlite.SQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs

class SharedDatabaseTest {

    private lateinit var sut: SharedDatabase

    @Before
    fun before() {
        sut = spyk(SharedDatabase(mockk()))
    }

    @Test
    fun `on create`() {
        val db = mockk<SQLiteDatabase>(relaxUnitFun = true)
        sut.onCreate(db)
        verify(exactly = 1) {
            db.execSQL(NotificationHistoryEntity.CREATE_TABLE)
        }
    }

    @Test
    fun `upgrade from v1 to v3`() {
        val db = mockk<SQLiteDatabase>(relaxUnitFun = true)
        sut.onUpgrade(db, 1, 3)
        verify(exactly = 1) {
            db.execSQL(NotificationHistoryEntity.ADD_JOURNEY_ID)
            db.execSQL(NotificationHistoryEntity.ADD_CAMPAIGN_TYPE)
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_AREA)
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_ACTION)
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_VALUE)
            db.execSQL(NotificationHistoryEntity.ADD_BUTTON_TYPE)
        }
    }

    @Test
    fun `upgrade from v2 to v3`() {
        val db = mockk<SQLiteDatabase>(relaxUnitFun = true)
        sut.onUpgrade(db, 2, 3)
        verify(exactly = 0) { db.execSQL(NotificationHistoryEntity.ADD_JOURNEY_ID) }
        verify(exactly = 1) {
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_AREA)
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_ACTION)
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_VALUE)
            db.execSQL(NotificationHistoryEntity.ADD_BUTTON_TYPE)
        }
    }

    @Test
    fun `execute with readonly value to false`() {
        val db = mockk<SQLiteDatabase>()
        every { sut.writableDatabase } returns db
        sut.execute(readOnly = false) {
            expectThat(it) isSameInstanceAs db
        }
        verify(exactly = 1) { sut.close() }
    }

    @Test
    fun `execute with readonly value to true`() {
        val db = mockk<SQLiteDatabase>()
        every { sut.readableDatabase } returns db
        sut.execute(readOnly = true) {
            expectThat(it) isSameInstanceAs db
        }
        verify(exactly = 1) { sut.close() }
    }

    @Test
    fun `execute with transaction value to true`() {
        val db = mockk<SQLiteDatabase>(relaxUnitFun = true)
        every { sut.writableDatabase } returns db
        sut.execute(readOnly = false, transaction = true) {
            expectThat(it) isSameInstanceAs db
        }
        verify(exactly = 1) {
            db.beginTransaction()
            db.setTransactionSuccessful()
            db.endTransaction()
            sut.close()
        }
    }
}