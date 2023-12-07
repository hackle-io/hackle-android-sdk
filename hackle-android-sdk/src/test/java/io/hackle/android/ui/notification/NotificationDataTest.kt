package io.hackle.android.ui.notification

import android.content.Intent
import android.os.Bundle
import io.hackle.android.internal.utils.toJson
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationDataTest {

    @Test
    fun `parse notification data`() {
        val intent = mockk<Intent>()
        val bundle = Bundle()
        bundle.putString("google.message_id", "abcd1234")
        bundle.putLong("google.sent_time", 1234567890)
        val map = mapOf<String, Any>(
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "campaignId" to 3333,
            "showForeground" to true,
            "colorFilter" to "#FFFFFF",
            "title" to "foo",
            "body" to "bar",
            "thumbnailImageUrl" to "https://foo.com",
            "largeImageUrl" to "https://bar.com",
            "link" to "foo://bar"
        )
        bundle.putString("hackle", map.toJson())
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.messageId, `is`("abcd1234"))
        assertThat(result.fcmSentTime, `is`(1234567890))
        assertThat(result.workspaceId, `is`(1111))
        assertThat(result.environmentId, `is`(2222))
        assertThat(result.campaignId, `is`(3333))
        assertThat(result.showForeground, `is`(true))
        assertThat(result.iconColorFilter, `is`("#FFFFFF"))
        assertThat(result.title, `is`("foo"))
        assertThat(result.body, `is`("bar"))
        assertThat(result.thumbnailImageUrl, `is`("https://foo.com"))
        assertThat(result.largeImageUrl, `is`("https://bar.com"))
        assertThat(result.link, `is`("foo://bar"))
    }

    @Test
    fun `should parse notification data even though optional fields are empty`() {
        val intent = mockk<Intent>()
        val bundle = Bundle()
        bundle.putString("google.message_id", "abcd1234")
        val map = mapOf<String, Any>(
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "campaignId" to 3333
        )
        bundle.putString("hackle", map.toJson())
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.messageId, `is`("abcd1234"))
        assertThat(result.fcmSentTime, `is`(0))
        assertThat(result.workspaceId, `is`(1111))
        assertThat(result.environmentId, `is`(2222))
        assertThat(result.campaignId, `is`(3333))
        assertThat(result.showForeground, `is`(false))
        assertNull(result.iconColorFilter)
        assertNull(result.title)
        assertNull(result.body)
        assertNull(result.thumbnailImageUrl)
        assertNull(result.largeImageUrl)
        assertNull(result.link)
    }

    @Test
    fun `should return null if hackle string is empty`() {
        val intent = mockk<Intent>()
        val bundle = Bundle()
        bundle.putString("google.message_id", "abcd1234")
        bundle.putLong("google.sent_time", 1234567890)
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if message id is empty`() {
        val intent = mockk<Intent>()
        val bundle = Bundle()
        bundle.putLong("google.sent_time", 1234567890)
        val map = mapOf<String, Any>(
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "campaignId" to 3333,
            "showForeground" to true,
            "colorFilter" to "#FFFFFF",
            "title" to "foo",
            "body" to "bar",
            "thumbnailImageUrl" to "https://foo.com",
            "largeImageUrl" to "https://bar.com",
            "link" to "foo://bar"
        )
        bundle.putString("hackle", map.toJson())
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if workspace id is empty`() {
        val intent = mockk<Intent>()
        val bundle = Bundle()
        bundle.putString("google.message_id", "abcd1234")
        bundle.putLong("google.sent_time", 1234567890)
        val map = mapOf<String, Any>(
            "environmentId" to 2222,
            "campaignId" to 3333,
            "showForeground" to true,
            "colorFilter" to "#FFFFFF",
            "title" to "foo",
            "body" to "bar",
            "thumbnailImageUrl" to "https://foo.com",
            "largeImageUrl" to "https://bar.com",
            "link" to "foo://bar"
        )
        bundle.putString("hackle", map.toJson())
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if environment id is empty`() {
        val intent = mockk<Intent>()
        val bundle = Bundle()
        bundle.putString("google.message_id", "abcd1234")
        bundle.putLong("google.sent_time", 1234567890)
        val map = mapOf<String, Any>(
            "workspaceId" to 1111,
            "campaignId" to 3333,
            "showForeground" to true,
            "colorFilter" to "#FFFFFF",
            "title" to "foo",
            "body" to "bar",
            "thumbnailImageUrl" to "https://foo.com",
            "largeImageUrl" to "https://bar.com",
            "link" to "foo://bar"
        )
        bundle.putString("hackle", map.toJson())
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if campaign id is empty`() {
        val intent = mockk<Intent>()
        val bundle = Bundle()
        bundle.putString("google.message_id", "abcd1234")
        bundle.putLong("google.sent_time", 1234567890)
        val map = mapOf<String, Any>(
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "showForeground" to true,
            "colorFilter" to "#FFFFFF",
            "title" to "foo",
            "body" to "bar",
            "thumbnailImageUrl" to "https://foo.com",
            "largeImageUrl" to "https://bar.com",
            "link" to "foo://bar"
        )
        bundle.putString("hackle", map.toJson())
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }
}