package io.hackle.android.ui.notification

import io.hackle.android.internal.database.shared.NotificationHistoryEntity

internal fun NotificationData.toEntity(clickTimestamp: Long) =
    NotificationHistoryEntity(
        historyId = 0,
        workspaceId = workspaceId,
        environmentId = environmentId,
        pushMessageId = pushMessageId,
        pushMessageKey = pushMessageKey,
        pushMessageExecutionId = pushMessageExecutionId,
        pushMessageDeliveryId = pushMessageDeliveryId,
        clickTimestamp = clickTimestamp,
        debug = debug
    )