package io.hackle.android.ui.notification

import android.content.Intent
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.ui.notification.Constants.DEFAULT_NOTIFICATION_CHANNEL_ID
import io.hackle.android.ui.notification.Constants.HIGH_NOTIFICATION_CHANNEL_ID
import io.hackle.android.ui.notification.Constants.KEY_BODY
import io.hackle.android.ui.notification.Constants.KEY_CAMPAIGN_TYPE
import io.hackle.android.ui.notification.Constants.KEY_CHANNEL_ID
import io.hackle.android.ui.notification.Constants.KEY_CHANNEL_TYPE
import io.hackle.android.ui.notification.Constants.KEY_CLICK_ACTION
import io.hackle.android.ui.notification.Constants.KEY_COLOR_FILTER
import io.hackle.android.ui.notification.Constants.KEY_DEBUG
import io.hackle.android.ui.notification.Constants.KEY_ENVIRONMENT_ID
import io.hackle.android.ui.notification.Constants.KEY_HACKLE
import io.hackle.android.ui.notification.Constants.KEY_JOURNEY_ID
import io.hackle.android.ui.notification.Constants.KEY_JOURNEY_KEY
import io.hackle.android.ui.notification.Constants.KEY_JOURNEY_NODE_ID
import io.hackle.android.ui.notification.Constants.KEY_LARGE_IMAGE_URL
import io.hackle.android.ui.notification.Constants.KEY_LINK
import io.hackle.android.ui.notification.Constants.KEY_MESSAGE_ID
import io.hackle.android.ui.notification.Constants.KEY_PUSH_MESSAGE_DELIVERY_ID
import io.hackle.android.ui.notification.Constants.KEY_PUSH_MESSAGE_EXECUTION_ID
import io.hackle.android.ui.notification.Constants.KEY_PUSH_MESSAGE_ID
import io.hackle.android.ui.notification.Constants.KEY_PUSH_MESSAGE_KEY
import io.hackle.android.ui.notification.Constants.KEY_SHOW_FOREGROUND
import io.hackle.android.ui.notification.Constants.KEY_THUMBNAIL_IMAGE_URL
import io.hackle.android.ui.notification.Constants.KEY_TITLE
import io.hackle.android.ui.notification.Constants.KEY_WORKSPACE_ID

internal enum class NotificationClickArea { MESSAGE, BUTTON }

internal data class NotificationData(
    val channelId: String,
    val messageId: String,
    val workspaceId: Long,
    val environmentId: Long,
    val pushMessageId: Long?,
    val pushMessageKey: Long?,
    val pushMessageExecutionId: Long?,
    val pushMessageDeliveryId: Long?,
    val showForeground: Boolean,
    val iconColorFilter: String?,
    val title: String?,
    val body: String?,
    val thumbnailImageUrl: String?,
    val largeImageUrl: String?,
    val clickAction: NotificationClickAction,
    val link: String?,
    val journeyId: Long?,
    val journeyKey: Long?,
    val journeyNodeId: Long?,
    val campaignType: String?,
    val debug: Boolean,
    val buttons: List<NotificationButton> = emptyList(),
    val clickArea: NotificationClickArea = NotificationClickArea.MESSAGE,
    val buttonType: String? = null,
) {

    val notificationId: Int
        get() = messageId.hashCode()

    val clickValue: String?
        get() = when (clickAction) {
            NotificationClickAction.APP_OPEN -> null
            NotificationClickAction.DEEP_LINK -> link
        }

    companion object {

        fun from(intent: Intent): NotificationData? {
            return from(intent, -1)
        }

        fun from(intent: Intent, actionIndex: Int): NotificationData? {
            try {
                val data = checkNotNull(intent.extras)
                val hackle = checkNotNull(data.getString(KEY_HACKLE))
                    .parseJson<Map<String, Any>>()
                val channelType = ChannelType.from(
                    hackle[KEY_CHANNEL_TYPE] as? String 
                        ?: ChannelType.HACKLE_DEFAULT.name
                )
                val buttons = parseButtons(hackle[Constants.KEY_BUTTONS])
                val button = buttons.getOrNull(actionIndex)
                return NotificationData(
                    channelId = channelId(channelType, hackle[KEY_CHANNEL_ID] as? String),
                    messageId = checkNotNull(data.getString(KEY_MESSAGE_ID)),
                    workspaceId = checkNotNull(hackle[KEY_WORKSPACE_ID] as? Number).toLong(),
                    environmentId = checkNotNull(hackle[KEY_ENVIRONMENT_ID] as? Number).toLong(),
                    pushMessageId = (hackle[KEY_PUSH_MESSAGE_ID] as? Number)?.toLong(),
                    pushMessageKey = (hackle[KEY_PUSH_MESSAGE_KEY] as? Number)?.toLong(),
                    pushMessageExecutionId = (hackle[KEY_PUSH_MESSAGE_EXECUTION_ID] as? Number)?.toLong(),
                    pushMessageDeliveryId = (hackle[KEY_PUSH_MESSAGE_DELIVERY_ID] as? Number)?.toLong(),
                    showForeground = hackle[KEY_SHOW_FOREGROUND] as? Boolean ?: false,
                    iconColorFilter = hackle[KEY_COLOR_FILTER] as? String,
                    title = hackle[KEY_TITLE] as? String,
                    body = hackle[KEY_BODY] as? String,
                    thumbnailImageUrl = hackle[KEY_THUMBNAIL_IMAGE_URL] as? String,
                    largeImageUrl = hackle[KEY_LARGE_IMAGE_URL] as? String,
                    clickAction = button?.clickAction ?: NotificationClickAction.from(
                        hackle[KEY_CLICK_ACTION] as? String
                            ?: NotificationClickAction.APP_OPEN.name
                    ),
                    link = if (button != null) button.link else hackle[KEY_LINK] as? String,
                    journeyId = (hackle[KEY_JOURNEY_ID] as? Number)?.toLong(),
                    journeyKey = (hackle[KEY_JOURNEY_KEY] as? Number)?.toLong(),
                    journeyNodeId = (hackle[KEY_JOURNEY_NODE_ID] as? Number)?.toLong(),
                    campaignType = hackle[KEY_CAMPAIGN_TYPE] as? String,
                    debug = hackle[KEY_DEBUG] as? Boolean ?: false,
                    buttons = buttons,
                    clickArea = if (button != null) NotificationClickArea.BUTTON else NotificationClickArea.MESSAGE,
                    buttonType = button?.type
                )
            } catch (_: Exception) {
            }
            return null
        }
        
        private fun parseButtons(value: Any?): List<NotificationButton> {
            val items = (value as? Map<*, *>)?.get(Constants.KEY_ITEMS) as? List<*> ?: return emptyList()
            return items.map { item ->
                val button = item as? Map<*, *> ?: return emptyList()
                val title = button[KEY_TITLE] as? String ?: return emptyList()
                val action = runCatching {
                    NotificationClickAction.from(button[KEY_CLICK_ACTION] as? String ?: NotificationClickAction.APP_OPEN.name)
                }.getOrDefault(NotificationClickAction.APP_OPEN)
                NotificationButton(
                    title = title,
                    clickAction = action,
                    link = button[KEY_LINK] as? String,
                    type = button[Constants.KEY_BUTTON_TYPE] as? String
                )
            }
        }

        fun channelId(channelType: ChannelType, channelId: String?): String {
            return when(channelType) {
                ChannelType.HACKLE_DEFAULT -> DEFAULT_NOTIFICATION_CHANNEL_ID
                ChannelType.HACKLE_HIGH -> HIGH_NOTIFICATION_CHANNEL_ID                
                ChannelType.CUSTOM -> {
                    if(channelId.isNullOrEmpty()) {
                        return DEFAULT_NOTIFICATION_CHANNEL_ID
                    }
                    return channelId
                }
            }
        }
    }
}
