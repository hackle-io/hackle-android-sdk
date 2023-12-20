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

internal fun NotificationData.toTrackEvent() =
    Event.Builder("\$push_click")
        .property("push_message_id", pushMessageId)
        .property("push_message_key", pushMessageKey)
        .property("push_message_execution_id", pushMessageExecutionId)
        .property("push_message_delivery_id", pushMessageDeliveryId)
        .property("debug", debug)
        .build()

internal fun NotificationEntity.toTrackEvent() =
    Event.Builder("\$push_click")
        .property("push_message_id", pushMessageId)
        .property("push_message_key", pushMessageKey)
        .property("push_message_execution_id", pushMessageExecutionId)
        .property("push_message_delivery_id", pushMessageDeliveryId)
        .property("debug", debug)
        .build()
