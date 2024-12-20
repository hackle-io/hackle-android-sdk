package io.hackle.android.internal.workspace.dto

import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionType.*
import io.hackle.sdk.core.model.InAppMessage.Behavior.CLICK
import io.hackle.sdk.core.model.InAppMessage.DisplayType.MODAL
import io.hackle.sdk.core.model.InAppMessage.LayoutType.IMAGE_ONLY
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Horizontal.LEFT
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Horizontal.RIGHT
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Vertical.BOTTOM
import io.hackle.sdk.core.model.InAppMessage.Message.Alignment.Vertical.TOP
import io.hackle.sdk.core.model.InAppMessage.Orientation.HORIZONTAL
import io.hackle.sdk.core.model.InAppMessage.Orientation.VERTICAL
import io.hackle.sdk.core.model.InAppMessage.PlatformType.ANDROID
import io.hackle.sdk.core.model.InAppMessage.PlatformType.IOS
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

internal class InAppMessageDtoTest {

    @Test
    fun `inAppMessage config test`() {
        val workspace = ResourcesWorkspaceFetcher("iam.json").fetch()

        val iam = workspace.getInAppMessageOrNull(1)!!

        expectThat(iam.id).isEqualTo(1)
        expectThat(iam.key).isEqualTo(1)
        expectThat(iam.status).isEqualTo(InAppMessage.Status.ACTIVE)
        expectThat(iam.period).isA<InAppMessage.Period.Custom>().and {
            get { startMillisInclusive } isEqualTo 42000
            get { endMillisExclusive } isEqualTo 43000
        }

        expectThat(iam.eventTrigger.frequencyCap!!.identifierCaps.size).isEqualTo(2)
        expectThat(iam.eventTrigger.frequencyCap!!.identifierCaps[0].identifierType).isEqualTo("\$sessionId")
        expectThat(iam.eventTrigger.frequencyCap!!.identifierCaps[0].count).isEqualTo(42)
        expectThat(iam.eventTrigger.frequencyCap!!.identifierCaps[1].identifierType).isEqualTo("\$deviceId")
        expectThat(iam.eventTrigger.frequencyCap!!.identifierCaps[1].count).isEqualTo(43)

        expectThat(iam.eventTrigger.frequencyCap!!.durationCap!!.durationMillis).isEqualTo(320 * 60 * 60 * 1000)
        expectThat(iam.eventTrigger.frequencyCap!!.durationCap!!.count).isEqualTo(420)

        expectThat(iam.eventTrigger.rules.size).isEqualTo(1)
        expectThat(iam.eventTrigger.rules[0].eventKey).isEqualTo("view_home")
        expectThat(iam.eventTrigger.rules[0].targets.size).isEqualTo(1)
        expectThat(iam.eventTrigger.rules[0].targets[0].conditions.size).isEqualTo(1)

        expectThat(iam.targetContext.targets.size).isEqualTo(1)
        expectThat(iam.targetContext.overrides.size).isEqualTo(1)
        expectThat(iam.targetContext.overrides[0].identifierType).isEqualTo("\$id")
        expectThat(iam.targetContext.overrides[0].identifiers).isEqualTo(listOf("user"))

        expectThat(iam.messageContext.platformTypes).isEqualTo(listOf(ANDROID, IOS))
        expectThat(iam.messageContext.orientations).isEqualTo(listOf(VERTICAL, HORIZONTAL))
        expectThat(iam.messageContext.messages.size).isEqualTo(1)
        expectThat(iam.messageContext.messages[0].lang).isEqualTo("ko")
        expectThat(iam.messageContext.messages[0].layout.displayType).isEqualTo(MODAL)
        expectThat(iam.messageContext.messages[0].layout.layoutType).isEqualTo(IMAGE_ONLY)
        expectThat(iam.messageContext.messages[0].layout.alignment?.horizontal).isEqualTo(LEFT)
        expectThat(iam.messageContext.messages[0].layout.alignment?.vertical).isEqualTo(TOP)

        expectThat(iam.messageContext.messages[0].images.size).isEqualTo(2)
        expectThat(iam.messageContext.messages[0].images[0].orientation).isEqualTo(VERTICAL)
        expectThat(iam.messageContext.messages[0].images[0].imagePath).isEqualTo("https://vertical-image.png")
        expectThat(iam.messageContext.messages[0].images[0].action?.behavior).isEqualTo(CLICK)
        expectThat(iam.messageContext.messages[0].images[0].action?.actionType).isEqualTo(WEB_LINK)
        expectThat(iam.messageContext.messages[0].images[0].action?.value).isEqualTo("https://www.hackle.io")
        expectThat(iam.messageContext.messages[0].images[1].orientation).isEqualTo(HORIZONTAL)
        expectThat(iam.messageContext.messages[0].images[1].imagePath).isEqualTo("https://horizontal-image.png")
        expectThat(iam.messageContext.messages[0].images[1].action?.behavior).isEqualTo(CLICK)
        expectThat(iam.messageContext.messages[0].images[1].action?.actionType).isEqualTo(WEB_LINK)
        expectThat(iam.messageContext.messages[0].images[1].action?.value).isEqualTo("https://www.hackle.io")

        expectThat(iam.messageContext.messages[0].imageAutoScroll).isEqualTo(InAppMessage.Message.ImageAutoScroll(42_000))

        expectThat(iam.messageContext.messages[0].text?.title?.text).isEqualTo("title_text")
        expectThat(iam.messageContext.messages[0].text?.title?.style?.textColor).isEqualTo("#0000FF")
        expectThat(iam.messageContext.messages[0].text?.body?.text).isEqualTo("body_text")
        expectThat(iam.messageContext.messages[0].text?.body?.style?.textColor).isEqualTo("#000000")

        expectThat(iam.messageContext.messages[0].buttons.size).isEqualTo(2)
        expectThat(iam.messageContext.messages[0].buttons[0].text).isEqualTo("close")
        expectThat(iam.messageContext.messages[0].buttons[0].style.textColor).isEqualTo("#000000")
        expectThat(iam.messageContext.messages[0].buttons[0].style.borderColor).isEqualTo("#FFFFFF")
        expectThat(iam.messageContext.messages[0].buttons[0].style.borderColor).isEqualTo("#FFFFFF")
        expectThat(iam.messageContext.messages[0].buttons[0].action.behavior).isEqualTo(CLICK)
        expectThat(iam.messageContext.messages[0].buttons[0].action.actionType).isEqualTo(HIDDEN)
        expectThat(iam.messageContext.messages[0].buttons[0].action.value).isEqualTo("")
        expectThat(iam.messageContext.messages[0].buttons[1].text).isEqualTo("apply")
        expectThat(iam.messageContext.messages[0].buttons[1].style.textColor).isEqualTo("#ffffff")
        expectThat(iam.messageContext.messages[0].buttons[1].style.bgColor).isEqualTo("#5e5af4")
        expectThat(iam.messageContext.messages[0].buttons[1].style.borderColor).isEqualTo("#FFFFFF")
        expectThat(iam.messageContext.messages[0].buttons[1].action.behavior).isEqualTo(CLICK)
        expectThat(iam.messageContext.messages[0].buttons[1].action.actionType).isEqualTo(WEB_LINK)
        expectThat(iam.messageContext.messages[0].buttons[1].action.value).isEqualTo("https://dashboard.hackle.io")

        expectThat(iam.messageContext.messages[0].background.color).isEqualTo("#FFFFFF")

        expectThat(iam.messageContext.messages[0].closeButton?.style?.textColor).isEqualTo("#000001")
        expectThat(iam.messageContext.messages[0].closeButton?.action?.behavior).isEqualTo(CLICK)
        expectThat(iam.messageContext.messages[0].closeButton?.action?.actionType).isEqualTo(CLOSE)

        expectThat(iam.messageContext.messages[0].action?.behavior).isEqualTo(CLICK)
        expectThat(iam.messageContext.messages[0].action?.actionType).isEqualTo(LINK_AND_CLOSE)
        expectThat(iam.messageContext.messages[0].action?.value).isEqualTo("https://www.hackle.io")

        expectThat(iam.messageContext.messages[0].outerButtons.size).isEqualTo(1)
        expectThat(iam.messageContext.messages[0].outerButtons[0].button.text).isEqualTo("outer")
        expectThat(iam.messageContext.messages[0].outerButtons[0].button.style.textColor).isEqualTo("#000000")
        expectThat(iam.messageContext.messages[0].outerButtons[0].button.style.bgColor).isEqualTo("#FFFFFF")
        expectThat(iam.messageContext.messages[0].outerButtons[0].button.style.borderColor).isEqualTo("#FFFFFF")
        expectThat(iam.messageContext.messages[0].outerButtons[0].button.action.behavior).isEqualTo(CLICK)
        expectThat(iam.messageContext.messages[0].outerButtons[0].button.action.actionType).isEqualTo(CLOSE)
        expectThat(iam.messageContext.messages[0].outerButtons[0].alignment.horizontal).isEqualTo(RIGHT)
        expectThat(iam.messageContext.messages[0].outerButtons[0].alignment.vertical).isEqualTo(BOTTOM)

        expectThat(iam.messageContext.messages[0].innerButtons.size).isEqualTo(1)
        expectThat(iam.messageContext.messages[0].innerButtons[0].button.text).isEqualTo("inner")
        expectThat(iam.messageContext.messages[0].innerButtons[0].button.style.textColor).isEqualTo("#000000")
        expectThat(iam.messageContext.messages[0].innerButtons[0].button.style.bgColor).isEqualTo("#FFFFFF")
        expectThat(iam.messageContext.messages[0].innerButtons[0].button.style.borderColor).isEqualTo("#FFFFFF")
        expectThat(iam.messageContext.messages[0].innerButtons[0].button.action.behavior).isEqualTo(CLICK)
        expectThat(iam.messageContext.messages[0].innerButtons[0].button.action.actionType).isEqualTo(CLOSE)
        expectThat(iam.messageContext.messages[0].innerButtons[0].alignment.horizontal).isEqualTo(RIGHT)
        expectThat(iam.messageContext.messages[0].innerButtons[0].alignment.vertical).isEqualTo(BOTTOM)
    }

    @Test
    fun `inAppMessage unsupported config test`() {
        val workspace = ResourcesWorkspaceFetcher("iam_invalid.json").fetch()
        expectThat(workspace.inAppMessages) {
            get { size } isEqualTo 0
        }

    }
}
