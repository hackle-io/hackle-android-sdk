package io.hackle.android.ui.notification

import android.content.Intent
import android.os.Bundle
import io.hackle.android.internal.utils.json.toJson
import io.hackle.android.mock.MockBundle
import io.hackle.android.ui.notification.Constants.DEFAULT_NOTIFICATION_CHANNEL_ID
import io.hackle.android.ui.notification.Constants.KEY_BODY
import io.hackle.android.ui.notification.Constants.KEY_CAMPAIGN_TYPE
import io.hackle.android.ui.notification.Constants.KEY_CHANNEL_ID
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
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFactoryTest {
    @Test
    fun `parse notification data with all fields`() {
        // Given
        val hackleData = mapOf(
            KEY_CHANNEL_ID to "test_channel",
            KEY_WORKSPACE_ID to 12345,
            KEY_ENVIRONMENT_ID to 67890,
            KEY_PUSH_MESSAGE_ID to 11111,
            KEY_PUSH_MESSAGE_KEY to 22222,
            KEY_PUSH_MESSAGE_EXECUTION_ID to 33333,
            KEY_PUSH_MESSAGE_DELIVERY_ID to 44444,
            KEY_SHOW_FOREGROUND to true,
            KEY_COLOR_FILTER to "#FF0000",
            KEY_TITLE to "Test Title",
            KEY_BODY to "Test Body",
            KEY_THUMBNAIL_IMAGE_URL to "https://example.com/thumb.jpg",
            KEY_LARGE_IMAGE_URL to "https://example.com/large.jpg",
            KEY_CLICK_ACTION to "DEEP_LINK",
            KEY_LINK to "https://example.com/deeplink",
            KEY_JOURNEY_ID to 55555,
            KEY_JOURNEY_KEY to 66666,
            KEY_JOURNEY_NODE_ID to 77777,
            KEY_CAMPAIGN_TYPE to "PUSH",
            KEY_DEBUG to true
        )

        val intent = createMockIntent("msg_123", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNotNull(notificationData)
        assertThat(notificationData!!.channelId, `is`("test_channel"))
        assertThat(notificationData.messageId, `is`("msg_123"))
        assertThat(notificationData.workspaceId, `is`(12345L))
        assertThat(notificationData.environmentId, `is`(67890L))
        assertThat(notificationData.pushMessageId, `is`(11111L))
        assertThat(notificationData.pushMessageKey, `is`(22222L))
        assertThat(notificationData.pushMessageExecutionId, `is`(33333L))
        assertThat(notificationData.pushMessageDeliveryId, `is`(44444L))
        assertTrue(notificationData.showForeground)
        assertThat(notificationData.iconColorFilter, `is`("#FF0000"))
        assertThat(notificationData.title, `is`("Test Title"))
        assertThat(notificationData.body, `is`("Test Body"))
        assertThat(notificationData.thumbnailImageUrl, `is`("https://example.com/thumb.jpg"))
        assertThat(notificationData.largeImageUrl, `is`("https://example.com/large.jpg"))
        assertThat(notificationData.clickAction, `is`(NotificationClickAction.DEEP_LINK))
        assertThat(notificationData.link, `is`("https://example.com/deeplink"))
        assertThat(notificationData.journeyId, `is`(55555L))
        assertThat(notificationData.journeyKey, `is`(66666L))
        assertThat(notificationData.journeyNodeId, `is`(77777L))
        assertThat(notificationData.campaignType, `is`("PUSH"))
        assertTrue(notificationData.debug)
    }

    @Test
    fun `parse notification data with minimal required fields`() {
        // Given
        val hackleData = mapOf(
            KEY_WORKSPACE_ID to 12345,
            KEY_ENVIRONMENT_ID to 67890,
            KEY_TITLE to "Test Title",
            KEY_BODY to "Test Body"
        )

        val intent = createMockIntent("msg_234", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNotNull(notificationData)
        assertThat(notificationData!!.channelId, `is`(DEFAULT_NOTIFICATION_CHANNEL_ID))
        assertThat(notificationData.messageId, `is`("msg_234"))
        assertThat(notificationData.workspaceId, `is`(12345L))
        assertThat(notificationData.environmentId, `is`(67890L))
        assertNull(notificationData.pushMessageId)
        assertNull(notificationData.pushMessageKey)
        assertNull(notificationData.pushMessageExecutionId)
        assertNull(notificationData.pushMessageDeliveryId)
        assertFalse(notificationData.showForeground)
        assertNull(notificationData.iconColorFilter)
        assertThat(notificationData.title, `is`("Test Title"))
        assertThat(notificationData.body, `is`("Test Body"))
        assertNull(notificationData.thumbnailImageUrl)
        assertNull(notificationData.largeImageUrl)
        assertThat(notificationData.clickAction, `is`(NotificationClickAction.APP_OPEN))
        assertNull(notificationData.link)
        assertNull(notificationData.journeyId)
        assertNull(notificationData.journeyKey)
        assertNull(notificationData.journeyNodeId)
        assertNull(notificationData.campaignType)
        assertFalse(notificationData.debug)
    }

    @Test
    fun `parse notification data with default channel id when not provided`() {
        // Given
        val hackleData = mapOf(
            KEY_WORKSPACE_ID to 12345,
            KEY_ENVIRONMENT_ID to 67890
        )

        val intent = createMockIntent("msg_345", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNotNull(notificationData)
        assertThat(notificationData!!.channelId, `is`(DEFAULT_NOTIFICATION_CHANNEL_ID))
    }

    @Test
    fun `parse notification data with default click action when not provided`() {
        // Given
        val hackleData = mapOf(
            KEY_WORKSPACE_ID to 12345,
            KEY_ENVIRONMENT_ID to 67890
        )

        val intent = createMockIntent("msg_123", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNotNull(notificationData)
        assertThat(notificationData!!.clickAction, `is`(NotificationClickAction.APP_OPEN))
    }

    @Test
    fun `parse notification data with various click actions`() {
        // Given
        val hackleData = mapOf(
            KEY_WORKSPACE_ID to 12345,
            KEY_ENVIRONMENT_ID to 67890,
            KEY_CLICK_ACTION to "DEEP_LINK"
        )

        val intent = createMockIntent("msg_123", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNotNull(notificationData)
        assertThat(notificationData!!.clickAction, `is`(NotificationClickAction.DEEP_LINK))
    }

    @Test
    fun `parse notification data should return null when intent extras is null`() {
        // Given
        val intent = mockk<Intent>()
        every { intent.extras } returns null

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNull(notificationData)
    }

    @Test
    fun `parse notification data should return null when hackle key is missing`() {
        // Given
        val bundle = Bundle().apply {
            putString(KEY_MESSAGE_ID, "msg_123")
        }
        val intent = mockk<Intent>()
        every { intent.extras } returns bundle

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNull(notificationData)
    }

    @Test
    fun `parse notification data should return null when message id is missing`() {
        // Given
        val hackleData = mapOf(
            KEY_WORKSPACE_ID to 12345,
            KEY_ENVIRONMENT_ID to 67890
        )
        val bundle = Bundle().apply {
            putString(KEY_HACKLE, hackleData.toJson())
        }
        val intent = mockk<Intent>()
        every { intent.extras } returns bundle

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNull(notificationData)
    }

    @Test
    fun `parse notification data should return null when workspace id is missing`() {
        // Given
        val hackleData = mapOf(
            KEY_ENVIRONMENT_ID to 67890
        )

        val intent = createMockIntent("msg_123", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNull(notificationData)
    }

    @Test
    fun `parse notification data should return null when environment id is missing`() {
        // Given
        val hackleData = mapOf(
            KEY_WORKSPACE_ID to 12345
        )

        val intent = createMockIntent("msg_123", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNull(notificationData)
    }

    @Test
    fun `parse notification data should return null when hackle json is invalid`() {
        // Given
        val intent = createMockIntent("msg_123", "invalid_json")

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNull(notificationData)
    }

    @Test
    fun `parse notification data with number fields as different types`() {
        val hackleData = mapOf(
            KEY_WORKSPACE_ID to 12345.0, 
            KEY_ENVIRONMENT_ID to 67890L, 
            KEY_PUSH_MESSAGE_ID to 11111, 
            KEY_JOURNEY_ID to 55555.0f
        )

        val intent = createMockIntent("msg_123", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNotNull(notificationData)
        assertThat(notificationData!!.workspaceId, `is`(12345L))
        assertThat(notificationData.environmentId, `is`(67890L))
        assertThat(notificationData.pushMessageId, `is`(11111L))
        assertThat(notificationData.journeyId, `is`(55555L))
    }

    @Test
    fun `parse notification data with boolean fields`() {
        // Given
        val hackleData = mapOf(
            KEY_WORKSPACE_ID to 12345,
            KEY_ENVIRONMENT_ID to 67890,
            KEY_SHOW_FOREGROUND to true,
            KEY_DEBUG to false
        )

        val intent = createMockIntent("msg_123", hackleData.toJson())

        // When
        val notificationData = NotificationData.from(intent)

        // Then
        assertNotNull(notificationData)
        assertTrue(notificationData!!.showForeground)
        assertFalse(notificationData.debug)
    }

    private fun createMockIntent(messageId: String, hackleJson: String): Intent {
        val bundle = MockBundle.create(
            mapOf(
                KEY_MESSAGE_ID to messageId,
                KEY_HACKLE to hackleJson
            )
        )
        val intent = mockk<Intent>()
        every { intent.extras } returns bundle
        return intent
    }
}
