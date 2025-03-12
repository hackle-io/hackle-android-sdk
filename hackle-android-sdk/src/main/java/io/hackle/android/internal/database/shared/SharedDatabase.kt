package io.hackle.android.internal.database.shared

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.hackle.android.internal.database.Database
import io.hackle.sdk.core.internal.log.Logger

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
        if(newVersion <= MAX_DATABASE_VERSION) {
            when (oldVersion) {
                1 -> {
                    // migration v1 -> v2
                    migrationTableFrom1To2(db)
                }
            }
        }
    }

    private fun migrationTableFrom1To2(db: SQLiteDatabase) {
        try {
            db.execSQL(NotificationHistoryEntity.ADD_JOURNEY_ID)
            db.execSQL(NotificationHistoryEntity.ADD_JOURNEY_KEY)
            db.execSQL(NotificationHistoryEntity.ADD_JOURNEY_NODE_ID)
            db.execSQL(NotificationHistoryEntity.ADD_CAMPAIGN_TYPE)
        } catch (e: Exception) {
            log.error { "Failed to upgrade database:\n$e" }
        }
    }

    companion object {

        const val DATABASE_NAME = "hackle_shared"
        const val DATABASE_VERSION = 2
        const val MAX_DATABASE_VERSION = 2
        private val log = Logger<SharedDatabase>()
    }
}