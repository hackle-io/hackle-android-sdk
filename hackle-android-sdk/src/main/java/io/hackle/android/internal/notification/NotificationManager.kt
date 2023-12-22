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
import kotlin.math.ceil

internal class NotificationManager(
    private val core: HackleCore,
    private val executor: Executor,
    private val workspaceFetcher: WorkspaceFetcher,
    private val userManager: UserManager,
    private val preferences: KeyValueRepository,
    private val repository: NotificationRepository,
) : NotificationDataReceiver, UserListener {

    private val flushing = AtomicBoolean(false)

    private var _registeredPushToken: String?
        get() = preferences.getString(KEY_FCM_TOKEN)
        private set(value) {
            if (value == null) {
                preferences.remove(KEY_FCM_TOKEN)
            } else {
                preferences.putString(KEY_FCM_TOKEN, value)
            }
        }
    val registeredPushToken: String?
        get() = _registeredPushToken

    fun setPushToken(fcmToken: String, timestamp: Long = System.currentTimeMillis()) {
        try {
            if (_registeredPushToken == fcmToken) {
                log.debug { "Provided same push token." }
                return
            }

            _registeredPushToken = fcmToken
            notifyPushTokenChanged(timestamp)
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
        notifyPushTokenChanged(timestamp)
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

            track(data.toTrackEvent(), timestamp)
        } catch (e: Exception) {
            log.error { "Failed to handle notification data: ${data.messageId}" }
        }
    }

    private fun notifyPushTokenChanged(timestamp: Long) {
        val fcmToken = preferences.getString(KEY_FCM_TOKEN)
        if (fcmToken.isNullOrEmpty()) {
            log.debug { "Push token is empty." }
            return
        }

        val event = RegisterPushTokenEvent(fcmToken).toTrackEvent()
        track(event, timestamp)
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

    private fun track(event: Event, timestamp: Long) {
        val hackleUser = userManager.toHackleUser(userManager.currentUser)
        core.track(event, hackleUser, timestamp)
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

                val totalCount = repository.count(
                    workspaceId = workspace.id,
                    environmentId = workspace.environmentId
                )
                val loop = ceil(totalCount.toDouble() / batchSize.toDouble()).toInt()
                log.debug { "Total notification data: $totalCount" }

                for (index in 0 until loop) {
                    val notifications = repository.getNotifications(
                        workspaceId = workspace.id,
                        environmentId = workspace.environmentId,
                        limit = batchSize
                    )

                    if (notifications.isEmpty()) {
                        break
                    }

                    for (notification in notifications) {
                        track(notification.toTrackEvent(), notification.clickTimestamp ?: System.currentTimeMillis())
                        log.debug { "Notification data[${notification.messageId}] successfully processed." }
                    }

                    repository.delete(notifications)
                    log.debug { "Flushed notification data: ${notifications.size} items" }

                    Thread.sleep(300L)
                }
            } catch (e: Exception) {
                log.debug { "Failed to flush notification data: $e" }
            } finally {
                flushing.set(false)
                log.debug { "Finished notification data flush task." }
            }
        }
    }

    companion object {

        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val DEFAULT_FLUSH_BATCH_SIZE = 5

        private val log = Logger<NotificationManager>()

    }
}