package io.hackle.android.internal.database.workspace

import android.database.sqlite.SQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs

class WorkspaceDatabaseTest {

    private lateinit var sut: WorkspaceDatabase

    @Before
    fun before() {
        sut = spyk(WorkspaceDatabase(mockk(), ""))
    }

    @Test
    fun `on create`() {
        val db = mockk<SQLiteDatabase>(relaxUnitFun = true)
        sut.onCreate(db)
        verify(exactly = 1) {
            db.execSQL(EventEntity.CREATE_TABLE)
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