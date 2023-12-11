package io.hackle.android.internal.notification

import io.hackle.android.internal.database.repository.NotificationRepository
import io.hackle.android.internal.user.UserManager
import io.hackle.android.ui.notification.NotificationData
import io.hackle.android.ui.notification.NotificationDataReceiver
import io.hackle.android.ui.notification.toDto
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import java.util.concurrent.Executor

internal class NotificationDataManager(
    private val core: HackleCore,
    private val executor: Executor,
    private val workspaceFetcher: WorkspaceFetcher,
    private val userManager: UserManager,
    private val repository: NotificationRepository,
) : NotificationDataReceiver {

    fun flush() {
        executor.execute(FlushTask())
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

    inner class FlushTask(
        private val batchSize: Int = 5
    ) : Runnable {

        override fun run() {
            try {
                log.debug { "Flushing notification data." }

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
                        repository.delete(notification.messageId)
                        log.debug { "Notification data[${notification.messageId}] successfully processed." }
                    }

                    Thread.sleep(300L)
                }
            } catch (e: Exception) {
                log.debug { "Failed to flush notification data: $e" }
            }
        }
    }

    companion object {

        private val log = Logger<NotificationDataManager>()

    }
}