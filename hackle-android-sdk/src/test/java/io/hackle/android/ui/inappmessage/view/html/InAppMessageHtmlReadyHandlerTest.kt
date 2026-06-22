package io.hackle.android.ui.inappmessage.view.html

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty

internal class InAppMessageHtmlReadyHandlerTest {

    @Test
    fun `when view is closed then bridge evaluation completion is ignored`() {
        val events = mutableListOf<String>()
        val sut = InAppMessageHtmlReadyHandler(
            isClosed = { true },
            requestFocus = { events.add("focus") },
            ready = { events.add("ready") }
        )

        sut.onBridgeEvaluated()

        expectThat(events).isEmpty()
    }
}
