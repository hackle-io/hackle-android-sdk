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
                    // migration v1 -> v2 -> v3
                    migrationTableFrom1To2(db)
                    migrationTableFrom2To3(db)
                }
                2 -> {
                    // migration v2 -> v3
                    migrationTableFrom2To3(db)
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

    private fun migrationTableFrom2To3(db: SQLiteDatabase) {
        try {
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_AREA)
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_ACTION)
            db.execSQL(NotificationHistoryEntity.ADD_CLICK_VALUE)
            db.execSQL(NotificationHistoryEntity.ADD_BUTTON_TYPE)
        } catch (e: Exception) {
            log.error { "Failed to upgrade database:\n$e" }
        }
    }

    companion object {

        const val DATABASE_NAME = "hackle_shared"
        const val DATABASE_VERSION = 3
        const val MAX_DATABASE_VERSION = 3
        private val log = Logger<SharedDatabase>()
    }
}