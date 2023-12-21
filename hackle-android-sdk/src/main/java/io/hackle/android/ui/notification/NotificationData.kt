package io.hackle.android.ui.notification

import android.content.Intent
import io.hackle.android.internal.utils.parseJson
import io.hackle.android.ui.notification.Constants.KEY_BODY
import io.hackle.android.ui.notification.Constants.KEY_CLICK_ACTION
import io.hackle.android.ui.notification.Constants.KEY_COLOR_FILTER
import io.hackle.android.ui.notification.Constants.KEY_DEBUG
import io.hackle.android.ui.notification.Constants.KEY_ENVIRONMENT_ID
import io.hackle.android.ui.notification.Constants.KEY_HACKLE
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

internal data class NotificationData(
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
    val debug: Boolean,
) {

    val notificationId: Int
        get() = messageId.hashCode()

    companion object {

        fun from(intent: Intent): NotificationData? {
            try {
                val data = checkNotNull(intent.extras)
                val hackle = checkNotNull(data.getString(KEY_HACKLE))
                    .parseJson<Map<String, Any>>()
                return NotificationData(
                    messageId = checkNotNull(data.getString(KEY_MESSAGE_ID)),
                    workspaceId = checkNotNull(hackle[KEY_WORKSPACE_ID] as? Number).toLong() ,
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
                    clickAction = NotificationClickAction.from(
                        hackle[KEY_CLICK_ACTION] as? String
                            ?: NotificationClickAction.APP_OPEN.text
                    ),
                    link = hackle[KEY_LINK] as? String,
                    debug = hackle[KEY_DEBUG] as? Boolean ?: false
                )
            } catch (_: Exception) { }
            return null
        }
    }
}
