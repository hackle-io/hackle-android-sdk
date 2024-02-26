package io.hackle.android.internal.notification

import io.hackle.android.internal.database.shared.NotificationHistoryEntity
import io.hackle.android.ui.notification.NotificationClickAction
import io.hackle.android.ui.notification.NotificationData
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class NotificationEventTest {

    @Test
    fun `notification data to event`() {
        val from = NotificationData(
            messageId = "abcd1234",
            workspaceId = 123L,
            environmentId = 456L,
            pushMessageId = 1111L,
            pushMessageKey = 2222L,
            pushMessageExecutionId = 3333L,
            pushMessageDeliveryId = 4444L,
            showForeground = true,
            iconColorFilter = "#FF00FF",
            title = "foo",
            body = "bar",
            thumbnailImageUrl = "https://foo.bar/image",
            largeImageUrl = "https://foo.foo",
            clickAction = NotificationClickAction.APP_OPEN,
            link = "foo://bar",
            debug = true
        )
        val to = from.toTrackEvent()
        assertThat(to.key, `is`("\$push_click"))
        assertThat(to.properties["push_message_id"], `is`(1111L))
        assertThat(to.properties["push_message_key"], `is`(2222L))
        assertThat(to.properties["push_message_execution_id"], `is`(3333L))
        assertThat(to.properties["push_message_delivery_id"], `is`(4444L))
        assertThat(to.properties["debug"], `is`(true))
    }

    @Test
    fun `notification history entity to event`() {
        val from = NotificationHistoryEntity(
            historyId = 0,
            workspaceId = 123L,
            environmentId = 456L,
            pushMessageId = 1111L,
            pushMessageKey = 2222L,
            pushMessageExecutionId = 3333L,
            pushMessageDeliveryId = 4444L,
            timestamp = 987654321L,
            debug = true,
        )
        val to = from.toTrackEvent()
        assertThat(to.key, `is`("\$push_click"))
        assertThat(to.properties["push_message_id"], `is`(1111L))
        assertThat(to.properties["push_message_key"], `is`(2222L))
        assertThat(to.properties["push_message_execution_id"], `is`(3333L))
        assertThat(to.properties["push_message_delivery_id"], `is`(4444L))
        assertThat(to.properties["debug"], `is`(true))
    }
}