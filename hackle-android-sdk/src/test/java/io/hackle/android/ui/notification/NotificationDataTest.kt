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

class NotificationDataTest {

    @Test
    fun `parse notification data`() {
        val intent = mockk<Intent>()
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
        val bundle = mockBundleOf(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890L,
            "hackle" to map.toJson(),
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.messageId, `is`("abcd1234"))
        assertThat(result.fcmSentTimestamp, `is`(1234567890))
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
        val map = mapOf<String, Any>(
            "workspaceId" to 1111,
            "environmentId" to 2222,
            "campaignId" to 3333
        )
        val bundle = mockBundleOf(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 0L,
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        val result = NotificationData.from(intent)
        assertNotNull(result)
        assertThat(result!!.messageId, `is`("abcd1234"))
        assertThat(result.fcmSentTimestamp, `is`(0))
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
        val bundle = mockBundleOf(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890
        ))
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if message id is empty`() {
        val intent = mockk<Intent>()
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
        val bundle = mockBundleOf(mapOf(
            "google.sent_time" to 1234567890,
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if workspace id is empty`() {
        val intent = mockk<Intent>()
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
        val bundle = mockBundleOf(mapOf(
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
        val bundle = mockBundleOf(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890,
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    @Test
    fun `should return null if campaign id is empty`() {
        val intent = mockk<Intent>()
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
        val bundle = mockBundleOf(mapOf(
            "google.message_id" to "abcd1234",
            "google.sent_time" to 1234567890,
            "hackle" to map.toJson()
        ))
        every { intent.extras } returns bundle

        assertNull(NotificationData.from(intent))
    }

    private fun mockBundleOf(map: Map<String, Any?>): Bundle {
        val bundle = mockk<Bundle>()
        for ((key, value) in map) {
            when (value) {
                is Boolean -> {
                    every { bundle.getBoolean(key) } returns value
                    every { bundle.getBoolean(key, any()) } returns value
                }
                is String -> {
                    every { bundle.getString(key) } returns value
                    every { bundle.getString(key, any()) } returns value
                }
                is Number -> {
                    every { bundle.getByte(key) } returns value.toByte()
                    every { bundle.getByte(key, any()) } returns value.toByte()

                    every { bundle.getChar(key) } returns value.toChar()
                    every { bundle.getChar(key, any()) } returns value.toChar()

                    every { bundle.getShort(key) } returns value.toShort()
                    every { bundle.getShort(key, any()) } returns value.toShort()

                    every { bundle.getInt(key) } returns value.toInt()
                    every { bundle.getInt(key, any()) } returns value.toInt()

                    every { bundle.getLong(key) } returns value.toLong()
                    every { bundle.getLong(key, any()) } returns value.toLong()

                    every { bundle.getFloat(key) } returns value.toFloat()
                    every { bundle.getFloat(key, any()) } returns value.toFloat()

                    every { bundle.getDouble(key) } returns value.toDouble()
                    every { bundle.getDouble(key, any()) } returns value.toDouble()
                }
                else -> throw UnsupportedOperationException("Type is not supported.")
            }
        }
        return bundle
    }
}