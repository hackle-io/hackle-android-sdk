package io.hackle.android.support

import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.ValueType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import java.util.UUID

internal object Targets {

    fun create(vararg conditions: Target.Condition): Target {
        return Target(conditions.toList())
    }

    fun condition(key: Target.Key = key(), match: Target.Match = match()): Target.Condition {
        return Target.Condition(key, match)
    }

    fun key(
        type: Target.Key.Type = Target.Key.Type.USER_PROPERTY,
        name: String = "name"
    ): Target.Key {
        return Target.Key(type, name)
    }

    fun match(
        type: Target.Match.Type = Target.Match.Type.MATCH,
        operator: Target.Match.Operator = Target.Match.Operator.IN,
        valueType: ValueType = ValueType.STRING,
        values: List<Any> = listOf("hackle")
    ): Target.Match {
        return Target.Match(type, operator, valueType, values)
    }
}

internal object InAppMessages {

    fun create(
        id: Long = 1,
        key: Long = 1,
        status: InAppMessage.Status = InAppMessage.Status.ACTIVE,
        period: InAppMessage.Period = InAppMessage.Period.Always,
        eventTrigger: InAppMessage.EventTrigger = eventTrigger(),
        targetContext: InAppMessage.TargetContext = targetContext(),
        messageContext: InAppMessage.MessageContext = messageContext()
    ): InAppMessage {
        return InAppMessage(
            id = id,
            key = key,
            status = status,
            period = period,
            eventTrigger = eventTrigger,
            targetContext = targetContext,
            messageContext = messageContext
        )
    }

    fun eventTrigger(
        rules: List<InAppMessage.EventTrigger.Rule> = listOf(
            InAppMessage.EventTrigger.Rule(
                "test",
                emptyList()
            )
        ),
        frequencyCap: InAppMessage.EventTrigger.FrequencyCap? = null
    ): InAppMessage.EventTrigger {
        return InAppMessage.EventTrigger(rules = rules, frequencyCap = frequencyCap)
    }

    fun frequencyCap(
        identifierCaps: List<InAppMessage.EventTrigger.IdentifierCap> = emptyList(),
        durationCap: InAppMessage.EventTrigger.DurationCap? = null
    ): InAppMessage.EventTrigger.FrequencyCap {
        return InAppMessage.EventTrigger.FrequencyCap(identifierCaps, durationCap)
    }

    fun identifierCap(
        identifierType: String = "\$id",
        count: Int = 1
    ): InAppMessage.EventTrigger.IdentifierCap {
        return InAppMessage.EventTrigger.IdentifierCap(identifierType, count)
    }

    fun durationCap(
        duration: Long = 60,
        count: Int = 1
    ): InAppMessage.EventTrigger.DurationCap {
        return InAppMessage.EventTrigger.DurationCap(duration, count)
    }

    fun targetContext(
        targets: List<Target> = emptyList(),
        overrides: List<InAppMessage.UserOverride> = emptyList()
    ): InAppMessage.TargetContext {
        return InAppMessage.TargetContext(targets, overrides)
    }

    fun messageContext(
        defaultLang: String = "ko",
        experimentContext: InAppMessage.ExperimentContext? = null,
        platformTypes: List<InAppMessage.PlatformType> = listOf(InAppMessage.PlatformType.ANDROID),
        orientations: List<InAppMessage.Orientation> = listOf(InAppMessage.Orientation.VERTICAL),
        messages: List<InAppMessage.Message> = listOf(message())
    ): InAppMessage.MessageContext {
        return InAppMessage.MessageContext(
            defaultLang,
            experimentContext,
            platformTypes,
            orientations,
            messages
        )
    }

    fun message(
        variationKey: String? = null,
        lang: String = "ko",
        layout: InAppMessage.Message.Layout = layout(),
        images: List<InAppMessage.Message.Image> = listOf(image()),
        imageAutoScroll: InAppMessage.Message.ImageAutoScroll? = null,
        text: InAppMessage.Message.Text? = text(),
        buttons: List<InAppMessage.Message.Button> = listOf(button()),
        closeButton: InAppMessage.Message.Button? = null,
        action: InAppMessage.Action? = null,
        outerButtons: List<InAppMessage.Message.PositionalButton> = emptyList(),
        innerButtons: List<InAppMessage.Message.PositionalButton> = emptyList(),
    ): InAppMessage.Message {
        return InAppMessage.Message(
            variationKey = variationKey,
            lang = lang,
            layout = layout,
            images = images,
            imageAutoScroll = imageAutoScroll,
            text = text,
            buttons = buttons,
            closeButton = closeButton,
            background = InAppMessage.Message.Background("#FFFFFF"),
            action = action,
            outerButtons = outerButtons,
            innerButtons = innerButtons,
        )
    }

    fun layout(
        displayType: InAppMessage.DisplayType = InAppMessage.DisplayType.MODAL,
        layoutType: InAppMessage.LayoutType = InAppMessage.LayoutType.IMAGE_ONLY,
        alignment: InAppMessage.Message.Alignment? = null
    ): InAppMessage.Message.Layout {
        return InAppMessage.Message.Layout(
            displayType = displayType,
            layoutType = layoutType,
            alignment = alignment
        )
    }

    fun action(
        behavior: InAppMessage.Behavior = InAppMessage.Behavior.CLICK,
        type: InAppMessage.ActionType = InAppMessage.ActionType.CLOSE,
        value: String? = null
    ): InAppMessage.Action {
        return InAppMessage.Action(
            behavior = behavior,
            actionType = type,
            value = value
        )
    }

    fun button(
        text: String = "button",
        textColor: String = "#000000",
        bgColor: String = "#FFFFFF",
        borderColor: String = "#FFFFFF",
        action: InAppMessage.Action = action()
    ): InAppMessage.Message.Button {
        return InAppMessage.Message.Button(
            text = text,
            style = InAppMessage.Message.Button.Style(
                textColor = textColor,
                bgColor = bgColor,
                borderColor = borderColor
            ),
            action = action
        )
    }

    fun image(
        orientation: InAppMessage.Orientation = InAppMessage.Orientation.VERTICAL,
        imagePath: String = "image_path",
        action: InAppMessage.Action? = null
    ): InAppMessage.Message.Image {
        return InAppMessage.Message.Image(
            orientation = orientation,
            imagePath = imagePath,
            action = action
        )
    }

    fun text(
        title: String = "title",
        titleColor: String = "#000000",
        body: String = "body",
        bodyColor: String = "#FFFFFF"
    ): InAppMessage.Message.Text {
        return InAppMessage.Message.Text(
            title = InAppMessage.Message.Text.Attribute(
                title,
                InAppMessage.Message.Text.Style(titleColor)
            ),
            body = InAppMessage.Message.Text.Attribute(
                body,
                InAppMessage.Message.Text.Style(bodyColor)
            )
        )
    }

    fun context(
        inAppMessage: InAppMessage = create(),
        message: InAppMessage.Message = message(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        properties: Map<String, Any> = mapOf(),
        triggerEventId: String = UUID.randomUUID().toString(),
        decisionReason: DecisionReason = DecisionReason.DEFAULT_RULE
    ): InAppMessagePresentationContext {
        return InAppMessagePresentationContext(inAppMessage, message, user, properties, triggerEventId, decisionReason)
    }
}
