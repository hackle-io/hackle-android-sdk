package io.hackle.android.ui.notification

import io.hackle.android.internal.database.shared.NotificationEntity

internal fun NotificationData.toDto() =
    NotificationEntity(
        messageId = messageId,
        workspaceId = workspaceId,
        environmentId = environmentId,
        campaignId = campaignId,
        fcmSentTime = fcmSentTime,
        clickAction = clickAction.text,
        link = link
    )