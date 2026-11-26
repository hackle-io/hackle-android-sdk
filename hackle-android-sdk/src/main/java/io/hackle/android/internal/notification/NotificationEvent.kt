package io.hackle.android.internal.notification

import io.hackle.android.internal.database.shared.NotificationHistoryEntity
import io.hackle.android.ui.notification.NotificationData
import io.hackle.sdk.common.Event

internal fun NotificationData.toTrackEvent() =
    Event.Builder("\$push_click")
        .property("push_message_id", pushMessageId)
        .property("push_message_key", pushMessageKey)
        .property("push_message_execution_id", pushMessageExecutionId)
        .property("push_message_delivery_id", pushMessageDeliveryId)
        .property("journey_id", journeyId)
        .property("journey_key", journeyKey)
        .property("journey_node_id", journeyNodeId)
        .property("campaign_type", campaignType)
        .property("debug", debug)
        .property("push_message_click_area", clickArea.name)
        .property("push_message_click_action", clickAction.name)
        .property("push_message_click_value", clickValue)
        .property("push_message_button_type", buttonType)
        .build()

internal fun NotificationHistoryEntity.toTrackEvent() =
    Event.Builder("\$push_click")
        .property("push_message_id", pushMessageId)
        .property("push_message_key", pushMessageKey)
        .property("push_message_execution_id", pushMessageExecutionId)
        .property("push_message_delivery_id", pushMessageDeliveryId)
        .property("journey_id", journeyId)
        .property("journey_key", journeyKey)
        .property("journey_node_id", journeyNodeId)
        .property("campaign_type", campaignType)
        .property("debug", debug)
        .property("push_message_click_area", clickArea)
        .property("push_message_click_action", clickAction)
        .property("push_message_click_value", clickValue)
        .property("push_message_button_type", buttonType)
        .build()
