package io.hackle.android.ui.inappmessage.view

import io.hackle.android.HackleApp
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.invocator.model.EventDto
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.sdk.common.Event
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

internal class InAppMessageViewJavascriptInterfaceTest {

    private fun sut(event: Event): InAppMessageViewJavascriptInterface {
        val context = mockk<InAppMessagePresentationContext>(relaxed = true) {
            every { triggerEvent } returns event
        }
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { presentationContext } returns context
        }
        return InAppMessageViewJavascriptInterface(
            app = mockk<HackleApp>(relaxed = true),
            view = view,
        )
    }

    @Test
    fun `getInAppMessageTriggerEvent - 정상 직렬화`() {
        val event = Event.builder("purchase")
            .value(99.99)
            .property("productId", "P123")
            .build()

        val json = sut(event).getInAppMessageTriggerEvent()

        expectThat(json) {
            contains("\"key\":\"purchase\"")
            contains("\"value\":99.99")
            contains("\"productId\":\"P123\"")
        }
    }

    @Test
    fun `getInAppMessageTriggerEvent - value가 null이면 JSON에서 누락`() {
        val event = Event.builder("login").build()

        val json = sut(event).getInAppMessageTriggerEvent()

        expectThat(json) {
            contains("\"key\":\"login\"")
            not { contains("\"value\"") }
            not { contains("\"properties\"") }
        }
    }

    @Test
    fun `getInAppMessageTriggerEvent - NaN value는 누락`() {
        val event = Event.builder("nan").value(Double.NaN).build()
        expectThat(sut(event).getInAppMessageTriggerEvent()).not { contains("\"value\"") }
    }

    @Test
    fun `getInAppMessageTriggerEvent - Infinity value는 누락`() {
        val event = Event.builder("inf").value(Double.POSITIVE_INFINITY).build()
        expectThat(sut(event).getInAppMessageTriggerEvent()).not { contains("\"value\"") }
    }

    @Test
    fun `getInAppMessageTriggerEvent - 빈 properties는 JSON에서 누락`() {
        val event = Event.builder("ev").value(1.0).build()
        expectThat(sut(event).getInAppMessageTriggerEvent()).not { contains("\"properties\"") }
    }

    @Test
    fun `getInAppMessageTriggerEvent - JSON parse 후 single quote 값이 원형 복원된다`() {
        val event = Event.builder("ev").property("name", "O'Brien").build()

        val json = sut(event).getInAppMessageTriggerEvent()
        val parsed = json.parseJson<EventDto>()

        expectThat(parsed.properties).isNotNull()
            .get { this["name"] } isEqualTo "O'Brien"
    }

    @Test
    fun `getInAppMessageTriggerEvent - JSON parse 후 backslash 값이 원형 복원된다`() {
        val event = Event.builder("ev").property("path", "a\\b").build()

        val json = sut(event).getInAppMessageTriggerEvent()
        val parsed = json.parseJson<EventDto>()

        expectThat(parsed.properties).isNotNull()
            .get { this["path"] } isEqualTo "a\\b"
    }
}
