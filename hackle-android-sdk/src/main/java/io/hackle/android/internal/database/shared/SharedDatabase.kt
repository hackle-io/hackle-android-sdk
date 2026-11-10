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
        if (oldVersion < 2 && newVersion >= 2) migrationTableFrom1To2(db)
        if (oldVersion < 3 && newVersion >= 3) migrationTableFrom2To3(db)
        if (oldVersion < 4 && newVersion >= 4) {
            with(NotificationHistoryEntity) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CLICK_AREA TEXT")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_BUTTON_TYPE TEXT")
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
        db.execSQL(NotificationHistoryEntity.ADD_ACTION_TYPE)
        db.execSQL(NotificationHistoryEntity.ADD_ACTION_VALUE)
    }

    companion object {

        const val DATABASE_NAME = "hackle_shared"
        const val DATABASE_VERSION = 4
        private val log = Logger<SharedDatabase>()
    }
}
