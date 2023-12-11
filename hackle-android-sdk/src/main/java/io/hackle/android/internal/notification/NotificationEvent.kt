package io.hackle.android.internal.notification

import io.hackle.android.internal.database.shared.NotificationEntity
import io.hackle.android.ui.notification.NotificationData
import io.hackle.sdk.common.Event

internal data class RegisterPushTokenEvent(val fcmToken: String)

internal fun RegisterPushTokenEvent.toTrackEvent() =
    Event.Builder("\$push_token")
        .property("fcm_token", fcmToken)
        .build()

internal fun NotificationData.toTrackEvent(clickTimestamp: Long) =
    Event.Builder("\$push_click")
        .property("message_id", messageId)
        .property("campaign_id", campaignId)
        .property("fcm_sent_timestamp", fcmSentTimestamp)
        .property("click_action", clickAction.text)
        .property("click_timestamp", clickTimestamp)
        .property("link", link)
        .build()

internal fun NotificationEntity.toTrackEvent() =
    Event.Builder("\$push_click")
        .property("message_id", messageId)
        .property("campaign_id", campaignId)
        .property("fcm_sent_timestamp", fcmSentTimestamp)
        .property("click_action", clickAction)
        .property("click_timestamp", clickTimestamp)
        .property("link", link)
        .build()
