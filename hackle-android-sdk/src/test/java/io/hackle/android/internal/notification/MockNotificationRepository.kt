package io.hackle.android.internal.notification

import io.hackle.android.internal.database.repository.NotificationRepository
import io.hackle.android.internal.database.shared.NotificationEntity

internal class MockNotificationRepository : NotificationRepository {

    private val workspaceData: MutableMap<String, MutableMap<String, NotificationEntity>> = HashMap()

    private fun getWorkspaceKey(workspaceId: Long, environmentId: Long) = "$workspaceId:$environmentId"

    private fun getWorkspaceData(workspaceId: Long, environmentId: Long) =
        workspaceData.getOrPut(getWorkspaceKey(workspaceId, environmentId)) { HashMap() }

    override fun count(workspaceId: Long, environmentId: Long): Int {
        val data = getWorkspaceData(workspaceId, environmentId)
        return data.count()
    }

    override fun save(entity: NotificationEntity) {
        val data = getWorkspaceData(entity.workspaceId, entity.environmentId)
        data[entity.messageId] = entity
    }

    fun putAll(entities: List<NotificationEntity>) {
        entities.forEach {
            val data = getWorkspaceData(it.workspaceId, it.environmentId)
            data[it.messageId] = it
        }
    }

    override fun getNotifications(
        workspaceId: Long,
        environmentId: Long,
        limit: Int?
    ): List<NotificationEntity> {
        return getWorkspaceData(workspaceId, environmentId)
            .filterValues { it.workspaceId == workspaceId && it.environmentId == environmentId }
            .values
            .toList()
    }

    override fun delete(entities: List<NotificationEntity>) {
        for (entity in entities) {
            val data = getWorkspaceData(entity.workspaceId, entity.environmentId)
            data.remove(entity.messageId)
        }
    }
}