package io.hackle.android.ui.notification

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class NotificationClickActionTest {

    @Test
    fun `text to enum`() {
        assertThat(
            NotificationClickAction.from("APP_OPEN"),
            `is`(NotificationClickAction.APP_OPEN)
        )
        assertThat(
            NotificationClickAction.from("DEEP_LINK"),
            `is`(NotificationClickAction.DEEP_LINK)
        )
    }

    @Test(expected = Throwable::class)
    fun `unexpected text to enum`() {
        NotificationClickAction.from("foobar")
    }

    @Test
    fun `enum to text`() {
        assertThat(
            NotificationClickAction.APP_OPEN.text,
            `is`("APP_OPEN")
        )
        assertThat(
            NotificationClickAction.DEEP_LINK.text,
            `is`("DEEP_LINK")
        )
    }
}