package io.hackle.android.internal.notification

import io.hackle.android.internal.database.repository.NotificationHistoryRepository
import io.hackle.android.internal.database.shared.NotificationHistoryEntity

internal class MockNotificationHistoryRepository : NotificationHistoryRepository {

    private val workspaceData: MutableMap<String, MutableMap<Long, NotificationHistoryEntity>> = HashMap()

    private fun getWorkspaceKey(workspaceId: Long, environmentId: Long) = "$workspaceId:$environmentId"

    private fun getWorkspaceData(workspaceId: Long, environmentId: Long) =
        workspaceData.getOrPut(getWorkspaceKey(workspaceId, environmentId)) { HashMap() }

    override fun count(workspaceId: Long, environmentId: Long): Int {
        val data = getWorkspaceData(workspaceId, environmentId)
        return data.count()
    }

    override fun save(entity: NotificationHistoryEntity) {
        val data = getWorkspaceData(entity.workspaceId, entity.environmentId)
        data[entity.historyId] = entity
    }

    fun putAll(entities: List<NotificationHistoryEntity>) {
        entities.forEach {
            val data = getWorkspaceData(it.workspaceId, it.environmentId)
            data[it.historyId] = it
        }
    }

    override fun getEntities(
        workspaceId: Long,
        environmentId: Long,
        limit: Int?
    ): List<NotificationHistoryEntity> {
        return getWorkspaceData(workspaceId, environmentId)
            .filterValues { it.workspaceId == workspaceId && it.environmentId == environmentId }
            .values
            .toList()
    }

    override fun delete(entities: List<NotificationHistoryEntity>) {
        for (entity in entities) {
            val data = getWorkspaceData(entity.workspaceId, entity.environmentId)
            data.remove(entity.historyId)
        }
    }
}