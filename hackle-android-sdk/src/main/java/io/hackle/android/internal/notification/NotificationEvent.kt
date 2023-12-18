package io.hackle.android.internal.notification

import io.hackle.android.internal.database.shared.NotificationEntity
import io.hackle.android.ui.notification.NotificationData
import io.hackle.sdk.common.Event

internal data class RegisterPushTokenEvent(val token: String)

internal fun RegisterPushTokenEvent.toTrackEvent(
    providerType: NotificationProviderType = NotificationProviderType.FIREBASE_CLOUD_MESSAGING
) = Event.Builder("\$push_token")
        .property("provider_type", providerType.text)
        .property("token", token)
        .build()

internal fun NotificationData.toTrackEvent(clickTimestamp: Long) =
    Event.Builder("\$push_click")
        .property("message_id", messageId)
        .property("push_message_id", pushMessageId)
        .property("fcm_sent_timestamp", fcmSentTimestamp)
        .property("click_action", clickAction.text)
        .property("click_timestamp", clickTimestamp)
        .property("link", link)
        .build()

internal fun NotificationEntity.toTrackEvent() =
    Event.Builder("\$push_click")
        .property("message_id", messageId)
        .property("push_message_id", pushMessageId)
        .property("fcm_sent_timestamp", fcmSentTimestamp)
        .property("click_action", clickAction)
        .property("click_timestamp", clickTimestamp)
        .property("link", link)
        .build()
