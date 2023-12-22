package io.hackle.android.ui.notification

import io.hackle.android.internal.database.shared.NotificationEntity

internal fun NotificationData.toEntity(clickTimestamp: Long) =
    NotificationEntity(
        notificationId = 0,
        messageId = messageId,
        workspaceId = workspaceId,
        environmentId = environmentId,
        pushMessageId = pushMessageId,
        pushMessageKey = pushMessageKey,
        pushMessageExecutionId = pushMessageExecutionId,
        pushMessageDeliveryId = pushMessageDeliveryId,
        clickTimestamp = clickTimestamp,
        debug = debug
    )