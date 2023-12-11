package io.hackle.android.ui.notification

import io.hackle.android.internal.database.repository.NotificationRepository
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.Executor

internal interface NotificationDataReceiver {

    fun onNotificationDataReceived(data: NotificationData, timestamp: Long)
}

internal class DefaultNotificationDataReceiver(
    private val executor: Executor,
    private val repository: NotificationRepository,
) : NotificationDataReceiver {

    override fun onNotificationDataReceived(data: NotificationData, timestamp: Long) {
        saveInLocal(data, timestamp)
    }

    private fun saveInLocal(data: NotificationData, timestamp: Long) {
        executor.execute {
            try {
                val entity = data.toDto(timestamp)
                repository.save(entity)
                log.debug { "Saved notification data: ${entity.messageId}[${entity.clickTimestamp}]" }
            } catch (e: Exception) {
                log.debug { "Notification data save error: $e" }
            }
        }
    }

    companion object {

        private val log = Logger<DefaultNotificationDataReceiver>()
    }
}