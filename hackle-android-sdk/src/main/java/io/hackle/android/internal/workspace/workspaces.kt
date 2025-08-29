package io.hackle.android.internal.workspace

import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.utils.enumValueOfOrNull
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.TargetingType.*
import java.util.concurrent.TimeUnit

private val log = Logger<WorkspaceImpl>()

// Experiment
internal fun ExperimentDto.toExperimentOrNull(type: Experiment.Type): Experiment? {
    return Experiment(
        id = id,
        key = key,
        name = name,
        type = type,
        identifierType = identifierType,
        status = Experiment.Status.fromExecutionStatusOrNull(execution.status) ?: return null,
        version = version,
        executionVersion = execution.version,
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


internal inline fun <reified E : Enum<E>> parseEnumOrNull(name: String): E? {
    val enum = enumValueOfOrNull<E>(name)
    if (enum == null) {
        log.debug { "Unsupported type[${E::class.java.name}.$name]. Please use the latest version of sdk." }
        return null
    }
    return enum
}

private inline fun <reified E : Enum<E>> parseEnumAllOrNull(names: List<String>): List<E>? {
    val enums = mutableListOf<E>()
    for (name in names) {
        val enum = parseEnumOrNull<E>(name) ?: return null
        enums.add(enum)
    }
    return enums
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
    val status = parseEnumOrNull<InAppMessage.Status>(status) ?: return null
    val period = when (timeUnit) {
        "IMMEDIATE" -> InAppMessage.Period.Always
        "CUSTOM" -> InAppMessage.Period.Custom(
            startMillisInclusive = startEpochTimeMillis ?: return null,
            endMillisExclusive = endEpochTimeMillis ?: return null
        )

        else -> return null
    }
    val messageContext = messageContext.toMessageContextOrNull() ?: return null

    val eventTriggerRules = eventTriggerRules.map { it.toTriggerRule() }
    val eventFrequencyCap = eventFrequencyCap?.toFrequencyCap()
    val eventTriggerDelay = eventTriggerDelay?.let { it.toDelayOrNull() ?: return null }
        ?: InAppMessage.Delay(InAppMessage.Delay.Type.IMMEDIATE, null)

    return InAppMessage(
        id = id,
        key = key,
        period = period,
        status = status,
        eventTrigger = InAppMessage.EventTrigger(
            rules = eventTriggerRules,
            frequencyCap = eventFrequencyCap,
            delay = eventTriggerDelay
        ),
        evaluateContext = InAppMessage.EvaluateContext(
            atDeliverTime = evaluationContext?.atDeliverTime ?: false
        ),
        targetContext = targetContext.toTargetContext(),
        messageContext = messageContext
    )

}

internal fun InAppMessageDto.EventTriggerRuleDto.toTriggerRule(): InAppMessage.EventTrigger.Rule {
    return InAppMessage.EventTrigger.Rule(
        eventKey = eventKey,
        targets = targets.mapNotNull { it.toTargetOrNull(PROPERTY) }
    )
}

internal fun InAppMessageDto.EventFrequencyCapDto.toFrequencyCap(): InAppMessage.EventTrigger.FrequencyCap {
    return InAppMessage.EventTrigger.FrequencyCap(
        identifierCaps = identifiers.map { it.toIdentifierCap() },
        durationCap = duration?.toDurationCapOrNull()
    )
}

internal fun InAppMessageDto.CampaignDelayDto.toDelayOrNull(): InAppMessage.Delay? {
    return InAppMessage.Delay(
        type = parseEnumOrNull<InAppMessage.Delay.Type>(type) ?: return null,
        afterCondition = afterConditionDto?.let { it.toAfterConditionOrNull() ?: return null }

    )
}

internal fun InAppMessageDto.CampaignDelayDto.AfterConditionDto.toAfterConditionOrNull(): InAppMessage.Delay.AfterCondition? {
    val timeUnit = parseEnumOrNull<TimeUnit>(duration.timeUnit) ?: return null
    return InAppMessage.Delay.AfterCondition(
        durationMillis = timeUnit.toMillis(duration.amount)
    )
}

internal fun InAppMessageDto.IdentifierCapDto.toIdentifierCap(): InAppMessage.EventTrigger.IdentifierCap {
    return InAppMessage.EventTrigger.IdentifierCap(
        identifierType = identifierType,
        count = countPerIdentifier.toInt()
    )
}

internal fun InAppMessageDto.DurationCapDto.toDurationCapOrNull(): InAppMessage.EventTrigger.DurationCap? {
    val timeUnit = parseEnumOrNull<TimeUnit>(durationUnit.timeUnit) ?: return null
    return InAppMessage.EventTrigger.DurationCap(
        durationMillis = timeUnit.toMillis(durationUnit.amount),
        count = countPerDuration.toInt()
    )
}

internal fun InAppMessageDto.TargetContextDto.toTargetContext(): InAppMessage.TargetContext {
    return InAppMessage.TargetContext(
        targets = targets.mapNotNull { it.toTargetOrNull(PROPERTY) },
        overrides = overrides.map { it.toUserOverride() }
    )
}

internal fun InAppMessageDto.TargetContextDto.UserOverrideDto.toUserOverride(): InAppMessage.UserOverride {
    return InAppMessage.UserOverride(
        identifiers = identifiers,
        identifierType = identifierType
    )
}

internal fun InAppMessageDto.MessageContextDto.toMessageContextOrNull(): InAppMessage.MessageContext? {
    val experimentContext = if (exposure.type == "AB_TEST" && exposure.key != null) {
        InAppMessage.ExperimentContext(exposure.key)
    } else {
        null
    }
    return InAppMessage.MessageContext(
        defaultLang = defaultLang,
        experimentContext = experimentContext,
        platformTypes = parseEnumAllOrNull(platformTypes) ?: return null,
        orientations = parseEnumAllOrNull(orientations) ?: return null,
        messages = messages.map { it.toMessageOrNull() ?: return null }
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.toMessageOrNull(): InAppMessage.Message? {
    return InAppMessage.Message(
        variationKey = variationKey,
        lang = lang,
        layout = layout.toLayoutOrNull() ?: return null,
        images = images.map { it.toImageOrNull() ?: return null },
        imageAutoScroll = imageAutoScroll?.let { it.toImageAutoScrollOrNull() ?: return null },
        text = text?.toText(),
        buttons = buttons.map { it.toButtonOrNull() ?: return null },
        closeButton = closeButton?.let { it.toButtonOrNull() ?: return null },
        background = InAppMessage.Message.Background(
            color = background.color
        ),
        action = action?.let { it.toActionOrNull() ?: return null },
        outerButtons = outerButtons.map { it.toPositionalButtonOrNull() ?: return null },
        innerButtons = innerButtons.map { it.toPositionalButtonOrNull() ?: return null }
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.LayoutDto.toLayoutOrNull(): InAppMessage.Message.Layout? {
    return InAppMessage.Message.Layout(
        displayType = parseEnumOrNull<InAppMessage.DisplayType>(displayType) ?: return null,
        layoutType = parseEnumOrNull<InAppMessage.LayoutType>(layoutType) ?: return null,
        alignment = alignment?.let { it.toAlignmentOrNull() ?: return null }
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.ImageDto.toImageOrNull(): InAppMessage.Message.Image? {
    return InAppMessage.Message.Image(
        orientation = parseEnumOrNull<InAppMessage.Orientation>(orientation) ?: return null,
        imagePath = imagePath,
        action = action?.let { it.toActionOrNull() ?: return null }
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.ImageAutoScrollDto.toImageAutoScrollOrNull(): InAppMessage.Message.ImageAutoScroll? {
    val timeUnit = parseEnumOrNull<TimeUnit>(interval.timeUnit) ?: return null
    return InAppMessage.Message.ImageAutoScroll(
        intervalMillis = timeUnit.toMillis(interval.amount)
    )
}

internal fun InAppMessageDto.MessageContextDto.ActionDto.toActionOrNull(): InAppMessage.Action? {
    return InAppMessage.Action(
        behavior = parseEnumOrNull<InAppMessage.Behavior>(behavior) ?: return null,
        actionType = parseEnumOrNull<InAppMessage.ActionType>(type) ?: return null,
        value = value
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.CloseButtonDto.toButtonOrNull(): InAppMessage.Message.Button? {
    return InAppMessage.Message.Button(
        text = "✕",
        style = InAppMessage.Message.Button.Style(
            textColor = style.color,
            bgColor = "#FFFFFF",
            borderColor = "#FFFFFF"
        ),
        action = action.toActionOrNull() ?: return null
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.ButtonDto.toButtonOrNull(): InAppMessage.Message.Button? {
    return InAppMessage.Message.Button(
        text = text,
        style = InAppMessage.Message.Button.Style(
            textColor = style.textColor,
            bgColor = style.bgColor,
            borderColor = style.borderColor
        ),
        action = action.toActionOrNull() ?: return null
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.TextDto.toText(): InAppMessage.Message.Text {
    return InAppMessage.Message.Text(
        title = title.toAttribute(),
        body = body.toAttribute()
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.TextDto.TextAttributeDto.toAttribute(): InAppMessage.Message.Text.Attribute {
    return InAppMessage.Message.Text.Attribute(
        text = text,
        style = InAppMessage.Message.Text.Style(
            textColor = style.textColor
        )
    )
}

internal fun InAppMessageDto.MessageContextDto.AlignmentDto.toAlignmentOrNull(): InAppMessage.Message.Alignment? {
    return InAppMessage.Message.Alignment(
        vertical = parseEnumOrNull<InAppMessage.Message.Alignment.Vertical>(vertical) ?: return null,
        horizontal = parseEnumOrNull<InAppMessage.Message.Alignment.Horizontal>(horizontal) ?: return null
    )
}

internal fun InAppMessageDto.MessageContextDto.MessageDto.PositionalButtonDto.toPositionalButtonOrNull(): InAppMessage.Message.PositionalButton? {
    return InAppMessage.Message.PositionalButton(
        button = button.toButtonOrNull() ?: return null,
        alignment = alignment.toAlignmentOrNull() ?: return null
    )
}
