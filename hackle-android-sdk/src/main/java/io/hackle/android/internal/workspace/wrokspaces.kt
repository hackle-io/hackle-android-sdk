package io.hackle.android.internal.workspace

import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.utils.enumValueOfOrNull
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.TargetingType.*

private val log = Logger<WorkspaceImpl>()

// Experiment
internal fun ExperimentDto.toExperimentOrNull(type: Experiment.Type): Experiment? {
    return Experiment(
        id = id,
        key = key,
        type = type,
        identifierType = identifierType,
        status = Experiment.Status.fromExecutionStatusOrNull(execution.status) ?: return null,
        version = version,
        variations = variations.map { it.toVariation() },
        userOverrides = execution.userOverrides.associate { it.userId to it.variationId },
        segmentOverrides = execution.segmentOverrides.mapNotNull { it.toTargetRuleOrNull(IDENTIFIER) },
        targetAudiences = execution.targetAudiences.mapNotNull { it.toTargetOrNull(PROPERTY) },
        targetRules = execution.targetRules.mapNotNull { it.toTargetRuleOrNull(PROPERTY) },
        defaultRule = execution.defaultRule.toActionOrNull() ?: return null,
        containerId = containerId,
        winnerVariationId = winnerVariationId
    )
}

internal fun VariationDto.toVariation() = Variation(
    id = id,
    key = key,
    isDropped = status == "DROPPED",
    parameterConfigurationId = parameterConfigurationId
)

internal fun TargetDto.toTargetOrNull(targetingType: TargetingType): Target? {
    val conditions = conditions.mapNotNull { it.toConditionOrNull(targetingType) }
    return if (conditions.isEmpty()) {
        null
    } else {
        Target(conditions)
    }
}

internal fun TargetDto.ConditionDto.toConditionOrNull(targetingType: TargetingType): Target.Condition? {
    val key = key.toTargetKeyOrNull() ?: return null

    if (!targetingType.supports(key.type)) {
        return null
    }

    return Target.Condition(
        key = key,
        match = match.toMatchOrNull() ?: return null
    )
}

internal fun TargetDto.KeyDto.toTargetKeyOrNull(): Target.Key? {
    return Target.Key(
        type = parseEnumOrNull<Target.Key.Type>(type) ?: return null,
        name = name
    )
}

internal fun TargetDto.MatchDto.toMatchOrNull(): Target.Match? {
    return Target.Match(
        type = parseEnumOrNull<Target.Match.Type>(type) ?: return null,
        operator = parseEnumOrNull<Target.Match.Operator>(operator) ?: return null,
        valueType = parseEnumOrNull<ValueType>(valueType) ?: return null,
        values = values
    )
}

internal fun TargetActionDto.toActionOrNull(): Action? {
    return when (type) {
        "VARIATION" -> Action.Variation(requireNotNull(variationId))
        "BUCKET" -> Action.Bucket(requireNotNull(bucketId))
        else -> {
            log.debug { "Unsupported action type[$type]. Please use the latest version of sdk" }
            return null
        }
    }
}

internal fun TargetRuleDto.toTargetRuleOrNull(targetingType: TargetingType): TargetRule? {
    return TargetRule(
        target = target.toTargetOrNull(targetingType) ?: return null,
        action = action.toActionOrNull() ?: return null,
    )
}


private inline fun <reified E : Enum<E>> parseEnumOrNull(name: String): E? {
    val enum = enumValueOfOrNull<E>(name)
    if (enum == null) {
        log.debug { "Unsupported type[${E::class.java.name}.$name]. Please use the latest version of sdk." }
        return null
    }
    return enum
}

// Bucket
internal fun BucketDto.toBucket() = Bucket(
    id = id,
    seed = seed,
    slotSize = slotSize,
    slots = slots.map { it.toSlot() }
)

internal fun SlotDto.toSlot() = Slot(
    startInclusive = startInclusive,
    endExclusive = endExclusive,
    variationId = variationId
)

// EventType
internal fun EventTypeDto.toEventType() = EventType.Custom(id, key)

// Segment
internal fun SegmentDto.toSegmentOrNull(): Segment? {
    return Segment(
        id = id,
        key = key,
        type = parseEnumOrNull<Segment.Type>(type) ?: return null,
        targets = targets.mapNotNull { it.toTargetOrNull(SEGMENT) }
    )
}

internal fun ContainerDto.toContainer() = Container(
    id = id,
    bucketId = bucketId,
    groups = groups.map { it.toContainerGroup() }
)

internal fun ContainerGroupDto.toContainerGroup() = ContainerGroup(
    id = id,
    experiments = experiments
)

internal fun ParameterConfigurationDto.toParameterConfiguration() = ParameterConfiguration(
    id = id,
    parameters = parameters.associate { it.key to it.value }
)

internal fun RemoteConfigParameterDto.toRemoteConfigParameterOrNull(): RemoteConfigParameter? {
    return RemoteConfigParameter(
        id = id,
        key = key,
        type = parseEnumOrNull<ValueType>(type) ?: return null,
        identifierType = identifierType,
        targetRules = targetRules.mapNotNull { it.toTargetRuleOrNull() },
        defaultValue = RemoteConfigParameter.Value(
            id = defaultValue.id,
            rawValue = defaultValue.value
        )
    )
}

internal fun RemoteConfigParameterDto.TargetRuleDto.toTargetRuleOrNull(): RemoteConfigParameter.TargetRule? {
    return RemoteConfigParameter.TargetRule(
        key = key,
        name = name,
        target = target.toTargetOrNull(PROPERTY) ?: return null,
        bucketId = bucketId,
        value = RemoteConfigParameter.Value(
            id = value.id,
            rawValue = value.value
        )
    )
}

internal fun InAppMessageDto.toInAppMessageOrNull(): InAppMessage? {
    val parsedTimeUnit = parseEnumOrNull<InAppMessage.TimeUnitType>(timeUnit) ?: let {
        log.info { "timeUnit NULL" }
        return null
    }

    val messageContext = messageContext.toMessageContextOrNull() ?: let {
        log.info { "messageContext NULL" }
        return null
    }
    val parsedStatus = parseEnumOrNull<InAppMessage.Status>(status) ?: let {
        log.info { "parsedStatus NULL" }
        return null
    }

    return InAppMessage(
        id = id,
        key = key,
        displayTimeRange = when (parsedTimeUnit) {
            InAppMessage.TimeUnitType.IMMEDIATE -> InAppMessage.Range.Immediate
            InAppMessage.TimeUnitType.CUSTOM -> InAppMessage.Range.Custom(
                startEpochTimeMillis ?: return null, endEpochTimeMillis ?: return null
            )
        },
        status = parsedStatus,
        eventTriggerRules = eventTriggerRules.map { it.toEventTriggerRule() },
        targetContext = targetContext.toTargetContext(),
        messageContext = messageContext
    )

}

internal fun InAppMessageDto.EventTriggerRuleDto.toEventTriggerRule() =
    InAppMessage.EventTriggerRule(
        eventKey = eventKey,
        targets = targets.mapNotNull { it.toTargetOrNull(IN_APP_MESSAGE) }
    )

internal fun InAppMessageDto.TargetContextDto.toTargetContext() = InAppMessage.TargetContext(
    targets = targets.mapNotNull { it.toTargetOrNull(IN_APP_MESSAGE) },
    overrides = overrides.map { it.toUserOverride() }
)

internal fun InAppMessageDto.TargetContextDto.UserOverrideDto.toUserOverride() =
    InAppMessage.TargetContext.UserOverride(
        identifiers = identifiers,
        identifierType = identifierType
    )

internal fun InAppMessageDto.MessageContextDto.toMessageContextOrNull(): InAppMessage.MessageContext? {

    val messages = messages.map {
        it.toMessageOrNull() ?: return null
    }
    val exposure = exposure.toExposureOrNull() ?: return null

    val platformType =
        platformTypes.map { InAppMessage.MessageContext.PlatformType.from(it) ?: return null }

    val orientations = orientations.map {
        InAppMessage.MessageContext.Orientation.from(it) ?: return null
    }

    return InAppMessage.MessageContext(
        defaultLang = defaultLang,
        platformTypes = platformType,
        exposure = exposure,
        messages = messages,
        orientations = orientations
    )
}


internal fun InAppMessageDto.MessageContextDto.MessageDto.ExposureDto.toExposureOrNull(): InAppMessage.MessageContext.Exposure? {
    val parsedType =
        parseEnumOrNull<InAppMessage.MessageContext.Exposure.Type>(type) ?: return null

    return InAppMessage.MessageContext.Exposure(
        type = parsedType,
        key = key,
    )
}


internal fun InAppMessageDto.MessageContextDto.MessageDto.toMessageOrNull(): InAppMessage.MessageContext.Message? {

    val buttons = buttons.map {
        it.toButtonOrNull() ?: return null
    }

    val images = images.map {
        it.toImageOrNull() ?: return null
    }

    val layout = layout.toLayoutOrNull() ?: return null

    val closeButton =
        if (closeButton == null) null else let { closeButton.toCloseButtonOrNull() ?: return null }

    return InAppMessage.MessageContext.Message(
        lang = lang,
        layout = layout,
        images = images,
        text = text?.toText(),
        buttons = buttons,
        background = background.toBackGround(),
        closeButton = closeButton
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.CloseButtonDto.toCloseButtonOrNull(): InAppMessage.MessageContext.Message.CloseButton? {
    val action = action.toActionOrNUll() ?: return null

    return InAppMessage.MessageContext.Message.CloseButton(
        style = style.toStyle(),
        action = action
    )
}


internal fun InAppMessageDto.MessageContextDto.MessageDto.CloseButtonDto.StyleDto.toStyle() =
    InAppMessage.MessageContext.Message.CloseButton.Style(
        color = color
    )

internal fun InAppMessageDto.MessageContextDto.MessageDto.BackgroundDto.toBackGround() =
    InAppMessage.MessageContext.Message.Background(
        color = color
    )

internal fun InAppMessageDto.MessageContextDto.MessageDto.ButtonDto.toButtonOrNull(): InAppMessage.MessageContext.Message.Button? {
    val action = action.toActionOrNUll() ?: return null

    return InAppMessage.MessageContext.Message.Button(
        text = text,
        style = style.toStyle(),
        action = action
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.ButtonDto.StyleDto.toStyle() =
    InAppMessage.MessageContext.Message.Button.Style(
        textColor = textColor,
        bgColor = bgColor,
        borderColor = borderColor
    )

internal fun InAppMessageDto.MessageContextDto.MessageDto.ImageDto.toImageOrNull(): InAppMessage.MessageContext.Message.Image? {
    val orientation =
        parseEnumOrNull<InAppMessage.MessageContext.Orientation>(orientation)
            ?: return null


    return InAppMessage.MessageContext.Message.Image(
        orientation = orientation,
        imagePath = imagePath,
        action = if (action != null) action.toActionOrNUll() ?: return null else null
    )

}

internal fun InAppMessageDto.MessageContextDto.ActionDto.toActionOrNUll(): InAppMessage.MessageContext.Action? {
    val parsedType = parseEnumOrNull<InAppMessage.MessageContext.Action.Type>(type) ?: return null
    val parsedBehavior =
        parseEnumOrNull<InAppMessage.MessageContext.Action.Behavior>(behavior) ?: return null

    return InAppMessage.MessageContext.Action(
        behavior = parsedBehavior,
        type = parsedType,
        value = value
    )
}


internal fun InAppMessageDto.MessageContextDto.MessageDto.TextDto.toText() =
    InAppMessage.MessageContext.Message.Text(
        title = title.toTextAttribute(),
        body = body.toTextAttribute()
    )

internal fun InAppMessageDto.MessageContextDto.MessageDto.TextDto.TextAttributeDto.toTextAttribute() =
    InAppMessage.MessageContext.Message.Text.TextAttribute(
        text = text,
        style = style.toStyle()
    )

internal fun InAppMessageDto.MessageContextDto.MessageDto.TextDto.StyleDto.toStyle() =
    InAppMessage.MessageContext.Message.Text.Style(
        textColor = textColor
    )

internal fun InAppMessageDto.MessageContextDto.MessageDto.LayoutDto.toLayoutOrNull(): InAppMessage.MessageContext.Message.Layout? {
    val parsedDisplayType =
        parseEnumOrNull<InAppMessage.MessageContext.Message.Layout.DisplayType>(displayType)
            ?: return null
    val parsedLayoutType =
        parseEnumOrNull<InAppMessage.MessageContext.Message.Layout.LayoutType>(layoutType)
            ?: return null

    return InAppMessage.MessageContext.Message.Layout(
        displayType = parsedDisplayType,
        layoutType = parsedLayoutType
    )
}


