package io.hackle.android.ui.notification

import android.content.Context
import android.content.Intent
import io.hackle.android.internal.database.DatabaseHelper
import io.hackle.android.internal.database.repository.NotificationRepository
import io.hackle.sdk.core.internal.log.Logger

internal class NotificationHandler(context: Context) {

    interface NotificationEventTracker {

        fun receive(data: NotificationData)
    }

    private val repository = NotificationRepository(DatabaseHelper.getSharedDatabase(context))

    fun handleNotificationData(data: NotificationData, timestamp: Long = System.currentTimeMillis()) {
        saveInLocal(data, timestamp)
    }

    private fun saveInLocal(data: NotificationData, timestamp: Long) {
        try {
            val entity = data.toDto(timestamp)
            repository.save(entity)
            log.debug { "Saved notification data: ${entity.messageId}[${entity.clickTimestamp}]" }
        } catch (e: Exception) {
            log.debug { "Notification data save error: $e" }
        }
    }

    companion object {

        private val log = Logger<NotificationHandler>()
        private var _instance: NotificationHandler? = null

        fun getInstance(context: Context): NotificationHandler {
            return _instance ?: synchronized(this) {
                _instance ?: NotificationHandler(context.applicationContext)
                    .also { _instance = it }
            }
        }

        fun isHackleIntent(intent: Intent): Boolean {
            val extras = intent.extras ?: return false
            return extras.containsKey(Constants.KEY_HACKLE)
        }
    }
}