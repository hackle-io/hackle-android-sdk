package io.hackle.android.internal.pushtoken

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class PushTokenEventTest {

    @Test
    fun `register push token event`() {
        val from = RegisterPushTokenEvent(
            token = "abcd1234"
        )
        val to = from.toTrackEvent()
        assertThat(to.key, `is`("\$push_token"))
        assertThat(to.properties["provider_type"], `is`("FCM"))
        assertThat(to.properties["token"], `is`("abcd1234"))
    }
}