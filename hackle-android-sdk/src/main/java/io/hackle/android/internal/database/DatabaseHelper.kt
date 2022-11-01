package io.hackle.android.internal.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class DatabaseHelper(
    context: Context,
    name: String,
    version: Int,
) : SQLiteOpenHelper(context, name, null, version) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(EventEntity.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

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

    companion object {

        private val INSTANCES = hashMapOf<String, DatabaseHelper>()

        @Synchronized
        operator fun get(context: Context, sdkKey: String): DatabaseHelper {
            val databaseName = databaseName(sdkKey)

            var databaseHelper = INSTANCES[databaseName]
            if (databaseHelper == null) {
                databaseHelper = DatabaseHelper(context, databaseName, DATABASE_VERSION)
                INSTANCES[databaseName] = databaseHelper
            }

            return databaseHelper
        }

        private fun databaseName(sdkKey: String): String {
            return "${DATABASE_NAME}_${sdkKey}"
        }

        private const val DATABASE_NAME = "io.hackle"
        private const val DATABASE_VERSION = 1
    }
}
