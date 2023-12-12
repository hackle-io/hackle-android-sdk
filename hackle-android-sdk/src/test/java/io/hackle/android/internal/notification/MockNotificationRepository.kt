package io.hackle.android.internal.notification

import io.hackle.android.internal.database.repository.NotificationRepository
import io.hackle.android.internal.database.shared.NotificationEntity

internal class MockNotificationRepository : NotificationRepository {

    private val data: MutableMap<String, NotificationEntity> = HashMap()

    override fun count(): Int {
        return data.count()
    }

    override fun save(entity: NotificationEntity) {
        data[entity.messageId] = entity
    }

    fun replaceAll(entities: List<NotificationEntity>) {
        data.clear()
        entities.forEach { data[it.messageId] = it }
    }

    override fun getNotifications(
        workspaceId: Long,
        environmentId: Long,
        limit: Int?
    ): List<NotificationEntity> {
        return data.filterValues { it.workspaceId == workspaceId && it.environmentId == environmentId }
            .values
            .toList()
    }

    override fun delete(entities: List<NotificationEntity>) {
        for (entity in entities) {
            data.remove(entity.messageId)
        }
    }
}