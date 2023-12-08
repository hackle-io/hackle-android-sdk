package io.hackle.android.ui.notification

import io.hackle.android.internal.database.shared.NotificationEntity

internal fun NotificationData.toDto(clickTimestamp: Long = System.currentTimeMillis()) =
    NotificationEntity(
        messageId = messageId,
        workspaceId = workspaceId,
        environmentId = environmentId,
        campaignId = campaignId,
        fcmSentTimestamp = fcmSentTimestamp,
        clickAction = clickAction.text,
        clickTimestamp = clickTimestamp,
        link = link
    )