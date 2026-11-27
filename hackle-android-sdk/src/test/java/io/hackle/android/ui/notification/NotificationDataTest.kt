package io.hackle.android.ui.notification

import android.content.Intent
import io.hackle.android.internal.utils.json.toJson
import io.hackle.android.mock.MockBundle
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDataTest {

    @Test
    fun `parse notification data`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "channelType" to "HACKLE_DEFAULT",
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "pushMessageId" to 3333,
            "pushMessageKey" to 4444,
            "pushMessageExecutionId" to 5555,
            "pushMessageDeliveryId" to 6666,
            "showForeground" to true,
            "colorFilter" to "#FFFFFF",
            "title" to "foo",
            "body" to "bar",
            "thumbnailImageUrl" to "https://foo.com",
            "largeImageUrl" to "https://bar.com",
            "link" to "foo://bar",
            "debug" to true
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890L,
            "hackle" to map.toJson(),
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.channelId, `is`(Constants.DEFAULT_NOTIFICATION_CHANNEL_ID))
        assertThat(result.messageId, `is`("abcd1234"))
        assertThat(result.workspaceId, `is`(1111L))
        assertThat(result.environmentId, `is`(2222L))
        assertThat(result.pushMessageId, `is`(3333L))
        assertThat(result.pushMessageKey, `is`(4444L))
        assertThat(result.pushMessageExecutionId, `is`(5555L))
        assertThat(result.pushMessageDeliveryId, `is`(6666L))
        assertThat(result.showForeground, `is`(true))
        assertThat(result.iconColorFilter, `is`("#FFFFFF"))
        assertThat(result.title, `is`("foo"))
        assertThat(result.body, `is`("bar"))
        assertThat(result.thumbnailImageUrl, `is`("https://foo.com"))
        assertThat(result.largeImageUrl, `is`("https://bar.com"))
        assertThat(result.link, `is`("foo://bar"))
        assertTrue(result.debug)
    }

    @Test
    fun `parse buttons and select clicked action`() {
        val intent = mockk<Intent>()
        val payload = mapOf<String, Any>(
            "workspaceId" to 1,
            "environmentId" to 2,
            "clickAction" to "APP_OPEN",
            "buttons" to mapOf(
                "categoryId" to "category",
                "items" to listOf(
                    mapOf("title" to "Open", "type" to "PRIMARY", "clickAction" to "DEEP_LINK", "link" to "https://example.com"),
                    mapOf("title" to "Later", "clickAction" to "APP_OPEN")
                )
            )
        )
        every { intent.extras } returns MockBundle.create(mapOf(
            "google.message_id" to "message",
            "hackle" to payload.toJson()
        ))

        val data = requireNotNull(NotificationData.from(intent))
        assertThat(data.buttons.size, `is`(2))
        assertThat(data.buttons[0].title, `is`("Open"))
        assertThat(data.clickArea, `is`(NotificationClickArea.MESSAGE))

        val clicked = requireNotNull(NotificationData.from(intent, 0))
        assertThat(clicked.clickAction, `is`(NotificationClickAction.DEEP_LINK))
        assertThat(clicked.clickArea, `is`(NotificationClickArea.BUTTON))
        assertThat(clicked.buttonType, `is`("PRIMARY"))
        assertThat(clicked.link, `is`("https://example.com"))

        val appOpen = requireNotNull(NotificationData.from(intent, 1))
        assertThat(appOpen.clickAction, `is`(NotificationClickAction.APP_OPEN))
        assertThat(appOpen.clickArea, `is`(NotificationClickArea.BUTTON))
        assertThat(appOpen.link, `is`(nullValue()))
        assertThat(appOpen.buttonType, `is`(nullValue()))

        assertThat(NotificationData.from(intent, 8), `is`(data))
    }

    @Test
    fun `override channel id when default channel type`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "channelId" to "my_custom_channel",
            "channelType" to "HACKLE_DEFAULT",
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "title" to "foo",
            "body" to "bar"
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890L,
            "hackle" to map.toJson(),
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.channelId, `is`(Constants.DEFAULT_NOTIFICATION_CHANNEL_ID))
        assertThat(result.messageId, `is`("abcd1234"))
        assertThat(result.workspaceId, `is`(1111L))
        assertThat(result.environmentId, `is`(2222L))
        assertThat(result.title, `is`("foo"))
        assertThat(result.body, `is`("bar"))
    }

    @Test
    fun `should parse notification data with HACKLE_HIGH channel type`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "channelType" to "HACKLE_HIGH",
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "title" to "High Priority",
            "body" to "Important message"
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.channelId, `is`(Constants.HIGH_NOTIFICATION_CHANNEL_ID))
    }

    @Test
    fun `should parse notification data with CUSTOM channel type`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "channelType" to "CUSTOM",
            "channelId" to "my_custom_channel",
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "title" to "Custom Channel",
            "body" to "Custom message"
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.channelId, `is`("my_custom_channel"))
    }

    @Test
    fun `should use default channel when CUSTOM channel type has empty channelId`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "channelType" to "CUSTOM",
            "channelId" to "",
            "workspaceId" to 1111,
            "environmentId" to 2222
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.channelId, `is`(Constants.DEFAULT_NOTIFICATION_CHANNEL_ID))
    }

    @Test
    fun `should use default channel when channelType is not specified`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "workspaceId" to 1111,
            "environmentId" to 2222
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.channelId, `is`(Constants.DEFAULT_NOTIFICATION_CHANNEL_ID))
    }

    @Test
    fun `should parse notification data even though optional fields are empty`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "channelId" to "hackle_heads_up_notification",
            "workspaceId" to 1111,
            "environmentId" to 2222
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 0L,
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.channelId, `is`(Constants.DEFAULT_NOTIFICATION_CHANNEL_ID))
        assertThat(result.messageId, `is`("abcd1234"))
        assertThat(result.workspaceId, `is`(1111L))
        assertThat(result.environmentId, `is`(2222L))
        assertNull(result.pushMessageId)
        assertNull(result.pushMessageKey)
        assertNull(result.pushMessageExecutionId)
        assertNull(result.pushMessageDeliveryId)
        assertThat(result.showForeground, `is`(false))
        assertNull(result.iconColorFilter)
        assertNull(result.title)
        assertNull(result.body)
        assertNull(result.thumbnailImageUrl)
        assertNull(result.largeImageUrl)
        assertNull(result.link)
        assertFalse(result.debug)
    }

    @Test
    fun `should return null if hackle string is empty`() {
        val intent = mockk<Intent>()
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890
        ))
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if workspace id is empty`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "channelId" to "hackle_heads_up_notification",
            "environmentId" to 2222,
            "pushMessageId" to 3333,
            "pushMessageKey" to 4444,
            "pushMessageExecutionId" to 5555,
            "pushMessageDeliveryId" to 6666,
            "showForeground" to true,
            "colorFilter" to "#FFFFFF",
            "title" to "foo",
            "body" to "bar",
            "thumbnailImageUrl" to "https://foo.com",
            "largeImageUrl" to "https://bar.com",
            "link" to "foo://bar",
            "debug" to true
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890,
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if environment id is empty`() {
        val intent = mockk<Intent>()
        val map = mapOf<String, Any>(
            "channelId" to "hackle_heads_up_notification",
            "workspaceId" to 1111,
            "pushMessageId" to 3333,
            "pushMessageKey" to 4444,
            "pushMessageExecutionId" to 5555,
            "pushMessageDeliveryId" to 6666,
            "showForeground" to true,
            "colorFilter" to "#FFFFFF",
            "title" to "foo",
            "body" to "bar",
            "thumbnailImageUrl" to "https://foo.com",
            "largeImageUrl" to "https://bar.com",
            "link" to "foo://bar",
            "debug" to true
        )
        val bundle = MockBundle.create(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890,
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }
}