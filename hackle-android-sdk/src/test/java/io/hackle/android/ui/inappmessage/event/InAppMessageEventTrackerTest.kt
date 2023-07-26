package io.hackle.android.ui.inappmessage.event

import io.hackle.android.support.InAppMessages
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
            properties = mapOf("decision_reason" to "IN_APP_MESSAGE_TARGET")
        )

        sut.track(context, InAppMessageEvent.Impression, 42)

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$in_app_impression"
                        get { properties["in_app_message_id"] } isEqualTo 42L
                        get { properties["in_app_message_key"] } isEqualTo 320L
                        get { properties["title_text"] } isEqualTo "text_title"
                        get { properties["body_text"] } isEqualTo "text_body"
                        get { properties["image_url"] } isEqualTo listOf("image_path")
                        get { properties["button_text"] } isEqualTo listOf("button_1", "button_2")
                        get { properties["decision_reason"] } isEqualTo "IN_APP_MESSAGE_TARGET"
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
            properties = mapOf("decision_reason" to "IN_APP_MESSAGE_TARGET")
        )

        sut.track(context, InAppMessageEvent.Close, 42)

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$in_app_close"
                        get { properties["in_app_message_id"] } isEqualTo 42L
                        get { properties["in_app_message_key"] } isEqualTo 320L
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
            properties = mapOf("decision_reason" to "IN_APP_MESSAGE_TARGET")
        )

        sut.track(context, InAppMessageEvent.Action(action, InAppMessage.ActionArea.BUTTON, "button_text_"), 42)

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$in_app_action"
                        get { properties["in_app_message_id"] } isEqualTo 42L
                        get { properties["in_app_message_key"] } isEqualTo 320L
                        get { properties["action_area"] } isEqualTo "BUTTON"
                        get { properties["action_type"] } isEqualTo "WEB_LINK"
                        get { properties["button_text"] } isEqualTo "button_text_"
                        get { properties["action_value"] } isEqualTo "button_link_click"
                    }
                },
                any(),
                42L
            )
        }
    }
}
