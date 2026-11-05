package io.hackle.android.ui.notification

import io.mockk.mockk
import org.junit.After
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class PendingNotificationLinkStorageTest {

    @After
    fun tearDown() {
        PendingNotificationLinkStorage.drain()
    }

    @Test
    fun `drain - returns null when nothing is stored`() {
        expectThat(PendingNotificationLinkStorage.drain()).isNull()
    }

    @Test
    fun `drain - returns the most recent link and clears it`() {
        val first = PendingNotificationLink(mockk(), mockk())
        val second = PendingNotificationLink(mockk(), mockk())

        PendingNotificationLinkStorage.store(first)
        PendingNotificationLinkStorage.store(second)

        expectThat(PendingNotificationLinkStorage.drain()).isSameInstanceAs(second)
        expectThat(PendingNotificationLinkStorage.drain()).isNull()
    }
}
