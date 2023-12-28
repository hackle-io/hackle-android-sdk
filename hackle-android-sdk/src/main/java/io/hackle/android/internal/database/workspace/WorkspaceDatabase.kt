package io.hackle.android.internal.database.workspace

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.hackle.android.internal.database.Database

internal class WorkspaceDatabase(
    context: Context,
    sdkKey: String,
) : Database(
    context = context,
    name = DATABASE_NAME_FORMAT.format(sdkKey),
    version = DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(EventEntity.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    companion object {

        const val DATABASE_NAME_FORMAT = "hackle_%s"
        const val DATABASE_VERSION = 1
    }
}