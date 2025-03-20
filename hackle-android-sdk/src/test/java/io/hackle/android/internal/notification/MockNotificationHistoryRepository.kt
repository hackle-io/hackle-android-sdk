package io.hackle.android.internal.notification

import io.hackle.android.internal.database.repository.NotificationHistoryRepository
import io.hackle.android.internal.database.shared.NotificationHistoryEntity
import io.hackle.android.ui.notification.NotificationData

internal class MockNotificationHistoryRepository : NotificationHistoryRepository {

    private var incrementKey: Long = 0L
    private val workspaceData: MutableMap<String, MutableMap<Long, NotificationHistoryEntity>> = HashMap()

    private fun getWorkspaceKey(workspaceId: Long, environmentId: Long) = "$workspaceId:$environmentId"

    private fun getWorkspaceData(workspaceId: Long, environmentId: Long) =
        workspaceData.getOrPut(getWorkspaceKey(workspaceId, environmentId)) { HashMap() }

    override fun count(workspaceId: Long, environmentId: Long): Int {
        val data = getWorkspaceData(workspaceId, environmentId)
        return data.count()
    }

    override fun save(data: NotificationData, timestamp: Long) {
        val map = getWorkspaceData(data.workspaceId, data.environmentId)
        val entity = NotificationHistoryEntity(
            historyId = incrementKey ++,
            workspaceId = data.workspaceId,
            environmentId = data.environmentId,
            pushMessageId = data.pushMessageId,
            pushMessageKey = data.pushMessageKey,
            pushMessageExecutionId = data.pushMessageExecutionId,
            pushMessageDeliveryId = data.pushMessageDeliveryId,
            timestamp = timestamp,
            journeyId = data.journeyId,
            journeyKey = data.journeyKey,
            journeyNodeId = data.journeyNodeId,
            campaignType = data.campaignType,
            debug = data.debug
        )
        map[entity.historyId] = entity
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
        val map = getWorkspaceData(workspaceId, environmentId)
        val list = map.values.toList()
        if (limit != null && list.size > limit) {
            return list.subList(0, limit - 1)
        }
        return list
    }

    override fun delete(entities: List<NotificationHistoryEntity>) {
        for (entity in entities) {
            val data = getWorkspaceData(entity.workspaceId, entity.environmentId)
            data.remove(entity.historyId)
        }
    }
}