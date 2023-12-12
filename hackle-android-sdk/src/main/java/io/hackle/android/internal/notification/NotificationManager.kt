package io.hackle.android.internal.notification

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.NotificationRepository
import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.user.UserManager
import io.hackle.android.ui.notification.NotificationData
import io.hackle.android.ui.notification.NotificationDataReceiver
import io.hackle.android.ui.notification.toDto
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal class NotificationManager(
    private val core: HackleCore,
    private val executor: Executor,
    private val workspaceFetcher: WorkspaceFetcher,
    private val userManager: UserManager,
    private val preferences: KeyValueRepository,
    private val repository: NotificationRepository,
) : NotificationDataReceiver, UserListener {

    private val flushing = AtomicBoolean(false)

    fun setPushToken(fcmToken: String) {
        try {
            val saved = preferences.getString(KEY_FCM_TOKEN)
            if (saved == fcmToken) {
                return
            }

            preferences.putString(KEY_FCM_TOKEN, fcmToken)
            notifyPushTokenChanged()
        } catch (e: Exception) {
            log.debug { "Failed to register FCM push token: $e" }
        }
    }

    fun flush() {
        if (flushing.getAndSet(true)) {
            return
        }

        executor.execute(FlushTask())
    }

    override fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long) {
        notifyPushTokenChanged()
    }

    override fun onNotificationDataReceived(data: NotificationData, timestamp: Long) {
        try {
            val workspace = workspaceFetcher.fetch()
            if (workspace == null ||
                workspace.id != data.workspaceId ||
                workspace.environmentId != data.environmentId) {
                if (workspace == null) {
                    log.debug { "Workspace data is empty." }
                } else {
                    log.debug { "Current environment(${workspace.id}:${workspace.environmentId}) is not same as notification environment(${data.workspaceId}:${data.environmentId})." }
                }
                saveInLocal(data, timestamp)
                return
            }

            track(data.toTrackEvent(timestamp))
        } catch (e: Exception) {
            log.error { "Failed to handle notification data: ${data.messageId}" }
        }
    }

    private fun notifyPushTokenChanged() {
        val fcmToken = preferences.getString(KEY_FCM_TOKEN)
        if (fcmToken.isNullOrEmpty()) {
            log.debug { "Push token is empty." }
            return
        }

        val event = RegisterPushTokenEvent(fcmToken).toTrackEvent()
        track(event)
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

    private fun track(event: Event) {
        val hackleUser = userManager.toHackleUser(userManager.currentUser)
        core.track(event, hackleUser, System.currentTimeMillis())
        log.debug { "${event.key} event queued." }
    }

    private inner class FlushTask(
        private val batchSize: Int = DEFAULT_FLUSH_BATCH_SIZE
    ) : Runnable {

        override fun run() {
            try {
                val workspace = workspaceFetcher.fetch()
                if (workspace == null) {
                    log.debug { "Workspace data is empty." }
                    return
                }

                while (true) {
                    val notifications = repository.getNotifications(
                        workspaceId = workspace.id,
                        environmentId = workspace.environmentId,
                        limit = batchSize
                    )

                    if (notifications.isEmpty()) {
                        break
                    }

                    for (notification in notifications) {
                        track(notification.toTrackEvent())
                        log.debug { "Notification data[${notification.messageId}] successfully processed." }
                    }

                    repository.delete(notifications)
                    Thread.sleep(300L)
                }
            } catch (e: Exception) {
                log.debug { "Failed to flush notification data: $e" }
            } finally {
                flushing.set(false)
                log.debug { "Flushed notification data." }
            }
        }
    }

    companion object {

        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val DEFAULT_FLUSH_BATCH_SIZE = 5

        private val log = Logger<NotificationManager>()

    }
}