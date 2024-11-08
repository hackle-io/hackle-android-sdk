package io.hackle.android.internal.notification

import io.hackle.android.internal.database.repository.NotificationHistoryRepository
import io.hackle.android.internal.user.UserManager
import io.hackle.android.ui.notification.NotificationData
import io.hackle.android.ui.notification.NotificationDataReceiver
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
    private val repository: NotificationHistoryRepository,
) : NotificationDataReceiver {

    private val flushing = AtomicBoolean(false)

    fun flush() {
        if (flushing.getAndSet(true)) {
            return
        }

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

            track(data.toTrackEvent(), userManager.currentUser, timestamp)
        } catch (e: Exception) {
            log.error { "Failed to handle notification data: ${data.messageId}" }
        }
    }

    private fun saveInLocal(data: NotificationData, timestamp: Long) {
        executor.execute {
            try {
                repository.save(data, timestamp)
                log.debug { "Saved notification data: ${data.pushMessageId}[${timestamp}]" }
            } catch (e: Exception) {
                log.debug { "Notification data save error: $e" }
            }
        }
    }

    private fun track(event: Event, user: User, timestamp: Long) {
        val hackleUser = userManager.toHackleUser(user)
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

                val user = userManager.currentUser
                val totalCount = repository.count(
                    workspaceId = workspace.id,
                    environmentId = workspace.environmentId
                )
                if (totalCount <= 0) {
                    log.debug { "Notification data is empty." }
                    return
                }

                val loop = ceil(totalCount.toDouble() / batchSize.toDouble()).toInt()
                log.debug { "Notification data: $totalCount" }

                for (index in 0 until loop) {
                    val notifications = repository.getEntities(
                        workspaceId = workspace.id,
                        environmentId = workspace.environmentId,
                        limit = batchSize
                    )

                    if (notifications.isEmpty()) {
                        break
                    }

                    for (notification in notifications) {
                        track(
                            event = notification.toTrackEvent(),
                            user = user,
                            timestamp = notification.timestamp
                        )
                        log.debug { "Notification data[id=${notification.historyId}] successfully processed." }
                    }

                    repository.delete(notifications)
                    log.debug { "Flushed notification data: ${notifications.size} items" }
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

        private const val DEFAULT_FLUSH_BATCH_SIZE = 5
        private val log = Logger<NotificationManager>()

    }
}