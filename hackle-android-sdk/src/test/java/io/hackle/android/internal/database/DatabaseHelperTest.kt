package io.hackle.android.internal.database

import android.database.sqlite.SQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

internal class DatabaseHelperTest {

    private lateinit var sut: DatabaseHelper

    @Before
    fun before() {
        sut = spyk(DatabaseHelper(mockk()))
    }

    @Test
    fun `onCreate`() {
        val db = mockk<SQLiteDatabase>(relaxUnitFun = true)
        sut.onCreate(db)
        verify(exactly = 1) { db.execSQL(EventEntity.CREATE_TABLE) }
    }

    @Test
    fun `write`() {
        val db = mockk<SQLiteDatabase>()
        every { sut.writableDatabase } returns db
        sut.write { expectThat(it) isSameInstanceAs db }
        verify(exactly = 1) { sut.close() }
    }

    @Test
    fun `read`() {
        val db = mockk<SQLiteDatabase>()
        every { sut.readableDatabase } returns db
        sut.read { expectThat(it) isSameInstanceAs db }
        verify(exactly = 1) { sut.close() }
    }

    @Test
    fun `runInTransaction`() {
        val db = mockk<SQLiteDatabase>(relaxUnitFun = true)
        every { sut.writableDatabase } returns db
        sut.runInTransaction { expectThat(it) isSameInstanceAs db }

        verify(exactly = 1) { db.beginTransaction() }
        verify(exactly = 1) { db.setTransactionSuccessful() }
        verify(exactly = 1) { db.endTransaction() }
        verify(exactly = 1) { sut.close() }
    }

    @Test
    fun `Synchronized`() {
        val db = mockk<SQLiteDatabase>(relaxUnitFun = true)
        every { sut.writableDatabase } returns db
        every { sut.readableDatabase } returns db


        val elapsedMillis = measureTimeMillis {

            val latch = CountDownLatch(3)
            thread {
                sut.read {
                    Thread.sleep(100)
                    latch.countDown()
                }
            }
            thread {
                sut.write {
                    Thread.sleep(100)
                    latch.countDown()
                }
            }
            thread {
                sut.runInTransaction {
                    Thread.sleep(100)
                    latch.countDown()
                }
            }
            latch.await()
        }

        expectThat(elapsedMillis).isGreaterThanOrEqualTo(300)
    }
}