package io.hackle.android.internal.database.shared

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.hackle.android.internal.database.Database

internal class SharedDatabase(
    context: Context
) : Database(
    context = context,
    name = DATABASE_NAME,
    version = DATABASE_VERSION 
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(NotificationHistoryEntity.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        
    }

    companion object {

        const val DATABASE_NAME = "hackle_shared"
        const val DATABASE_VERSION = 1
    }
}