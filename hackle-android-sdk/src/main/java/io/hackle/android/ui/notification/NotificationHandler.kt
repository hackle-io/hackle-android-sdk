package io.hackle.android.ui.notification

import android.content.Context
import android.content.Intent
import io.hackle.android.internal.database.DatabaseHelper
import io.hackle.android.internal.database.repository.NotificationHistoryRepositoryImpl
import io.hackle.android.internal.task.TaskExecutors

internal class NotificationHandler(context: Context) {

    // setNotificationDataReceiver는 init(eventExecutor) 스레드에서, handleNotificationData는 트램펄린 액티비티
    // 메인 스레드에서 호출되므로 가시성 보장을 위해 @Volatile.
    @Volatile
    private var receiver: NotificationDataReceiver =
        DefaultNotificationDataReceiver(
            executor = TaskExecutors.default(),
            repository = NotificationHistoryRepositoryImpl(DatabaseHelper.getSharedDatabase(context))
        )

    fun setNotificationDataReceiver(receiver: NotificationDataReceiver) {
        this.receiver = receiver
    }

    fun handleNotificationData(data: NotificationData, timestamp: Long = System.currentTimeMillis()) {
        receiver.onNotificationDataReceived(data, timestamp)
    }

    companion object {

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