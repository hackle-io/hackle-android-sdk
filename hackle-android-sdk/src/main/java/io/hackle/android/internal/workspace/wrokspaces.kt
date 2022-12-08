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
