package io.hackle.android.internal.notification

import io.hackle.android.internal.database.shared.NotificationEntity
import io.hackle.android.ui.notification.NotificationClickAction
import io.hackle.android.ui.notification.NotificationData
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class NotificationEventTest {

    @Test
    fun `register push token event`() {
        val from = RegisterPushTokenEvent(
            fcmToken = "abcd1234"
        )
        val to = from.toTrackEvent()
        assertThat(to.key, `is`("\$push_token"))
        assertThat(to.properties["fcm_token"], `is`("abcd1234"))
    }

    @Test
    fun `notification data to event`() {
        val from = NotificationData(
            messageId = "abcd1234",
            workspaceId = 123L,
            environmentId = 456L,
            campaignId = 789L,
            fcmSentTimestamp = 1234567890L,
            showForeground = true,
            iconColorFilter = "#FF00FF",
            title = "foo",
            body = "bar",
            thumbnailImageUrl = "https://foo.bar/image",
            largeImageUrl = "https://foo.foo",
            clickAction = NotificationClickAction.APP_OPEN,
            link = "foo://bar",
        )
        val to = from.toTrackEvent(987654321L)
        assertThat(to.key, `is`("\$push_click"))
        assertThat(to.properties["message_id"], `is`("abcd1234"))
        assertThat(to.properties["campaign_id"], `is`(789L))
        assertThat(to.properties["fcm_sent_timestamp"], `is`(1234567890L))
        assertThat(to.properties["click_action"], `is`("APP_OPEN"))
        assertThat(to.properties["click_timestamp"], `is`(987654321L))
        assertThat(to.properties["link"], `is`("foo://bar"))
    }

    @Test
    fun `notification entity to event`() {
        val from = NotificationEntity(
            messageId = "abcd1234",
            workspaceId = 123L,
            environmentId = 456L,
            campaignId = 789L,
            fcmSentTimestamp = 1234567890L,
            clickAction = "APP_OPEN",
            clickTimestamp = 987654321L,
            link = "foo://bar",
        )
        val to = from.toTrackEvent()
        assertThat(to.key, `is`("\$push_click"))
        assertThat(to.properties["message_id"], `is`("abcd1234"))
        assertThat(to.properties["campaign_id"], `is`(789L))
        assertThat(to.properties["fcm_sent_timestamp"], `is`(1234567890L))
        assertThat(to.properties["click_action"], `is`("APP_OPEN"))
        assertThat(to.properties["click_timestamp"], `is`(987654321L))
        assertThat(to.properties["link"], `is`("foo://bar"))
    }
}