package io.hackle.android.ui.inappmessage.event

import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.model.InAppMessage
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageEventTrackerTest {

    @RelaxedMockK
    private lateinit var core: HackleCore

    @InjectMockKs
    private lateinit var sut: InAppMessageEventTracker

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `impression`() {
        val message = InAppMessages.message(
            layout = InAppMessages.layout(displayType = InAppMessage.DisplayType.BANNER),
            images = listOf(InAppMessages.image(imagePath = "image_path")),
            text = InAppMessages.text(title = "text_title", body = "text_body"),
            buttons = listOf(
                InAppMessages.button(text = "button_1"),
                InAppMessages.button(text = "button_2")
            )
        )
        val inAppMessage = InAppMessages.create(
            id = 42,
            key = 320,
            messageContext = InAppMessages.messageContext(messages = listOf(message))
        )
        val context = InAppMessages.context(
            inAppMessage = inAppMessage,
            message = message,
            decisionReason = DecisionReason.OVERRIDDEN,
            properties = mapOf("\$trigger_event_insert_id" to "event_insert_id")
        )

        sut.track(context, InAppMessageEvent.Impression, 42)

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$in_app_impression"
                        get { properties } isEqualTo mapOf(
                            "in_app_message_id" to 42L,
                            "in_app_message_key" to 320L,
                            "in_app_message_display_type" to "BANNER",
                            "title_text" to "text_title",
                            "body_text" to "text_body",
                            "image_url" to listOf("image_path"),
                            "button_text" to listOf("button_1", "button_2"),
                            "decision_reason" to "OVERRIDDEN",
                            "\$trigger_event_insert_id" to "event_insert_id",
                        )
                    }
                },
                any(),
                42L
            )
        }
    }

    @Test
    fun `close`() {
        val message = InAppMessages.message(
            layout = InAppMessages.layout(displayType = InAppMessage.DisplayType.BOTTOM_SHEET),
            images = listOf(InAppMessages.image(imagePath = "image_path")),
            text = InAppMessages.text(title = "text_title", body = "text_body"),
            buttons = listOf(
                InAppMessages.button(text = "button_1"),
                InAppMessages.button(text = "button_2")
            )
        )
        val inAppMessage = InAppMessages.create(
            id = 42,
            key = 320,
            messageContext = InAppMessages.messageContext(messages = listOf(message))
        )
        val context = InAppMessages.context(
            inAppMessage = inAppMessage,
            message = message,
            decisionReason = DecisionReason.OVERRIDDEN,
            properties = mapOf("\$trigger_event_insert_id" to "event_insert_id")
        )

        sut.track(context, InAppMessageEvent.Close, 42)

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$in_app_close"
                        get { properties } isEqualTo mapOf(
                            "in_app_message_id" to 42L,
                            "in_app_message_key" to 320L,
                            "in_app_message_display_type" to "BOTTOM_SHEET",
                            "decision_reason" to "OVERRIDDEN",
                            "\$trigger_event_insert_id" to "event_insert_id",
                        )
                    }
                },
                any(),
                42L
            )
        }
    }

    @Test
    fun `action`() {
        val action = InAppMessages.action(type = InAppMessage.ActionType.WEB_LINK, value = "button_link_click")
        val message = InAppMessages.message(
            layout = InAppMessages.layout(displayType = InAppMessage.DisplayType.MODAL),
            images = listOf(InAppMessages.image(imagePath = "image_path")),
            text = InAppMessages.text(title = "text_title", body = "text_body"),
            buttons = listOf(
                InAppMessages.button(text = "button_1", action = action),
                InAppMessages.button(text = "button_2")
            )
        )
        val inAppMessage = InAppMessages.create(
            id = 42,
            key = 320,
            messageContext = InAppMessages.messageContext(messages = listOf(message))
        )
        val context = InAppMessages.context(
            inAppMessage = inAppMessage,
            message = message,
            decisionReason = DecisionReason.OVERRIDDEN,
            properties = mapOf("\$trigger_event_insert_id" to "event_insert_id")
        )

        sut.track(context, InAppMessageEvent.buttonAction(action, message.buttons[0]), 42)

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$in_app_action"
                        get { properties } isEqualTo mapOf(
                            "in_app_message_id" to 42L,
                            "in_app_message_key" to 320L,
                            "in_app_message_display_type" to "MODAL",
                            "action_area" to "BUTTON",
                            "action_type" to "WEB_LINK",
                            "button_text" to "button_1",
                            "action_value" to "button_link_click",
                            "decision_reason" to "OVERRIDDEN",
                            "\$trigger_event_insert_id" to "event_insert_id",
                        )
                    }
                },
                any(),
                42L
            )
        }
    }
}
