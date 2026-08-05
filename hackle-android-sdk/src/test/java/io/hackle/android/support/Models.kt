package io.hackle.android.support

import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.model.Target

internal typealias VariationKey = io.hackle.sdk.common.Variation

internal object Targets {

    fun create(vararg conditions: Target.Condition): Target {
        return Target(conditions.toList())
    }

    fun condition(
        key: Target.Key = key(),
        match: Target.Match = match(),
    ): Target.Condition {
        return Target.Condition(key, match)
    }

    fun key(
        type: Target.Key.Type = Target.Key.Type.USER_PROPERTY,
        name: String = "age",
    ): Target.Key {
        return Target.Key(type, name)
    }

    fun match(
        type: Target.Match.Type = Target.Match.Type.MATCH,
        operator: Target.Match.Operator = Target.Match.Operator.IN,
        valueType: ValueType = ValueType.STRING,
        values: List<Any> = listOf("hackle"),
    ): Target.Match {
        return Target.Match(type, operator, valueType, values)
    }
}

internal fun segment(
    id: Long = 1,
    key: String = "segment",
    type: Segment.Type = Segment.Type.USER_ID,
    targets: List<Target> = emptyList(),
): Segment {
    return Segment(id, key, type, targets)
}

internal fun bucket(
    id: Long = 1,
    seed: Int = 0,
    slotSize: Int = 10000,
    slots: List<Slot> = emptyList(),
): Bucket {
    return Bucket(id, seed, slotSize, slots)
}

internal fun slot(
    startInclusive: Int = 0,
    endExclusive: Int = 10000,
    variationId: Long = 1,
): Slot {
    return Slot(startInclusive, endExclusive, variationId)
}

internal fun container(
    id: Long = 1,
    bucketId: Long = 1,
    groups: List<ContainerGroup> = emptyList(),
): Container {
    return Container(id, bucketId, groups)
}

internal fun containerGroup(
    id: Long = 1,
    experiments: List<Long> = emptyList(),
): ContainerGroup {
    return ContainerGroup(id, experiments)
}

internal fun parameterConfiguration(
    id: Long = 1,
    parameters: Map<String, Any> = emptyMap(),
): ParameterConfiguration {
    return ParameterConfiguration(id, parameters)
}

internal fun cohort(
    id: Long = 1,
): Cohort {
    return Cohort(id)
}

internal fun targetEvent(
    eventKey: String = "test",
    stats: List<TargetEvent.Stat> = emptyList(),
    property: TargetEvent.Property? = null,
): TargetEvent {
    return TargetEvent(eventKey, stats, property)
}

internal fun targetEventStat(
    date: Long = 42,
    count: Int = 1,
): TargetEvent.Stat {
    return TargetEvent.Stat(date, count)
}

internal fun targetEventProperty(
    key: String = "key",
    type: Target.Key.Type = Target.Key.Type.EVENT_PROPERTY,
    value: Any = "value",
): TargetEvent.Property {
    return TargetEvent.Property(key, type, value)
}

internal fun identifier(
    type: String = "\$id",
    value: String = "user",
): Identifier {
    return Identifier(type, value)
}
