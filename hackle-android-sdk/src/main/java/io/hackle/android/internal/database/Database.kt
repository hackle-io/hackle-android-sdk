package io.hackle.android.internal.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal abstract class Database(
    context: Context,
    name: String,
    version: Int
) : SQLiteOpenHelper(context, name, null, version) {

    @Synchronized
    fun <T> execute(
        readOnly: Boolean = false,
        transaction: Boolean = false,
        command: (SQLiteDatabase) -> T,
    ): T {
        return try {
            val database = if (readOnly) readableDatabase else writableDatabase
            execute(transaction, database, command)
        } finally {
            close()
        }
    }

    private fun <T> execute(
        transaction: Boolean,
        database: SQLiteDatabase,
        command: (SQLiteDatabase) -> T,
    ): T {
        return if (transaction) {
            database.beginTransaction()
            try {
                command(database)
                    .also { database.setTransactionSuccessful() }
            } finally {
                database.endTransaction()
            }
        } else {
            command(database)
        }
    }
}