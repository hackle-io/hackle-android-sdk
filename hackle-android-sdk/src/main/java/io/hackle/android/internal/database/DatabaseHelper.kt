package io.hackle.android.internal.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(EventEntity.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    @Synchronized
    fun <T> write(block: (SQLiteDatabase) -> T): T {
        return try {
            block(writableDatabase)
        } finally {
            close()
        }
    }

    @Synchronized
    fun <T> read(block: (SQLiteDatabase) -> T): T {
        return try {
            block(readableDatabase)
        } finally {
            close()
        }
    }

    @Synchronized
    fun <T> runInTransaction(block: (SQLiteDatabase) -> T): T {
        return try {
            val db = writableDatabase
            db.beginTransaction()
            try {
                block(db)
                    .also { db.setTransactionSuccessful() }
            } finally {
                db.endTransaction()
            }
        } finally {
            close()
        }
    }

    companion object {
        private const val DATABASE_NAME = "hackle"
        private const val DATABASE_VERSION = 1
    }
}
