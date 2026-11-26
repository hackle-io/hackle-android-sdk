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
            channelId = "hackle_default_channel",
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
            journeyId = 5555L,
            journeyKey = 6666L,
            journeyNodeId = 7777L,
            campaignType = "JOURNEY",
            debug = true
        )
        val to = from.toTrackEvent()
        assertThat(to.key, `is`("\$push_click"))
        assertThat(to.properties["push_message_id"], `is`(1111L))
        assertThat(to.properties["push_message_key"], `is`(2222L))
        assertThat(to.properties["push_message_execution_id"], `is`(3333L))
        assertThat(to.properties["push_message_delivery_id"], `is`(4444L))
        assertThat(to.properties["debug"], `is`(true))
        assertThat(to.properties["push_message_click_area"], `is`("MESSAGE"))
        assertThat(to.properties["push_message_click_action"], `is`("APP_OPEN"))
        assertThat(to.properties.containsKey("push_message_click_value"), `is`(false))
    }

    @Test
    fun `notification data to event with deep link`() {
        val from = NotificationData(
            channelId = "hackle_default_channel",
            messageId = "abcd1234",
            workspaceId = 123L,
            environmentId = 456L,
            pushMessageId = 1111L,
            pushMessageKey = 2222L,
            pushMessageExecutionId = 3333L,
            pushMessageDeliveryId = 4444L,
            showForeground = true,
            iconColorFilter = null,
            title = "foo",
            body = "bar",
            thumbnailImageUrl = null,
            largeImageUrl = null,
            clickAction = NotificationClickAction.DEEP_LINK,
            link = "foo://bar",
            journeyId = null,
            journeyKey = null,
            journeyNodeId = null,
            campaignType = "PUSH_MESSAGE",
            debug = false
        )
        val to = from.toTrackEvent()
        assertThat(to.properties["push_message_click_action"], `is`("DEEP_LINK"))
        assertThat(to.properties["push_message_click_value"], `is`("foo://bar"))
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
            journeyId = 5555L,
            journeyKey = 6666L,
            journeyNodeId = 7777L,
            campaignType = "JOURNEY",
            timestamp = 987654321L,
            clickArea = "BUTTON",
            buttonType = "PRIMARY",
            debug = true,
        )
        val to = from.toTrackEvent()
        assertThat(to.key, `is`("\$push_click"))
        assertThat(to.properties["push_message_id"], `is`(1111L))
        assertThat(to.properties["push_message_key"], `is`(2222L))
        assertThat(to.properties["push_message_execution_id"], `is`(3333L))
        assertThat(to.properties["push_message_delivery_id"], `is`(4444L))
        assertThat(to.properties["push_message_click_area"], `is`("BUTTON"))
        assertThat(to.properties["push_message_button_type"], `is`("PRIMARY"))
        assertThat(to.properties["journey_id"], `is`(5555L))
        assertThat(to.properties["journey_key"], `is`(6666L))
        assertThat(to.properties["journey_node_id"], `is`(7777L))
        assertThat(to.properties["campaign_type"], `is`("JOURNEY"))
        assertThat(to.properties["debug"], `is`(true))
    }
}