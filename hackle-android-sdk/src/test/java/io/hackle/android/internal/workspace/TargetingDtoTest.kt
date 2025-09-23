package io.hackle.android.internal.workspace

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class TargetingDtoTest {

    @Test
    fun `TargetDto creation and properties`() {
        val conditionDto = TargetDto.ConditionDto(
            key = TargetDto.KeyDto(type = "USER_PROPERTY", name = "age"),
            match = TargetDto.MatchDto(type = "GT", operator = "GREATER_THAN", valueType = "NUMBER", values = listOf(18))
        )
        val targetDto = TargetDto(
            conditions = listOf(conditionDto)
        )

        expectThat(targetDto.conditions).isEqualTo(listOf(conditionDto))
    }

    @Test
    fun `ConditionDto creation and properties`() {
        val keyDto = TargetDto.KeyDto(
            type = "SEGMENT",
            name = "premium_users"
        )
        val matchDto = TargetDto.MatchDto(
            type = "IN",
            operator = "IN",
            valueType = "STRING",
            values = listOf("premium", "vip", "enterprise")
        )
        val conditionDto = TargetDto.ConditionDto(
            key = keyDto,
            match = matchDto
        )

        expectThat(conditionDto.key).isEqualTo(keyDto)
        expectThat(conditionDto.match).isEqualTo(matchDto)
    }

    @Test
    fun `KeyDto creation and properties`() {
        val keyDto1 = TargetDto.KeyDto(
            type = "USER_ID",
            name = "userId"
        )
        val keyDto2 = TargetDto.KeyDto(
            type = "CUSTOM_PROPERTY",
            name = "subscription_tier"
        )

        expectThat(keyDto1.type).isEqualTo("USER_ID")
        expectThat(keyDto1.name).isEqualTo("userId")
        expectThat(keyDto2.type).isEqualTo("CUSTOM_PROPERTY")
        expectThat(keyDto2.name).isEqualTo("subscription_tier")
    }

    @Test
    fun `MatchDto creation and properties`() {
        val matchDto1 = TargetDto.MatchDto(
            type = "EQ",
            operator = "EQUALS",
            valueType = "STRING",
            values = listOf("premium")
        )
        val matchDto2 = TargetDto.MatchDto(
            type = "BETWEEN",
            operator = "BETWEEN",
            valueType = "NUMBER",
            values = listOf(18, 65)
        )
        val matchDto3 = TargetDto.MatchDto(
            type = "CONTAINS",
            operator = "CONTAINS",
            valueType = "STRING",
            values = listOf("android", "mobile")
        )

        expectThat(matchDto1.type).isEqualTo("EQ")
        expectThat(matchDto1.operator).isEqualTo("EQUALS")
        expectThat(matchDto1.valueType).isEqualTo("STRING")
        expectThat(matchDto1.values).isEqualTo(listOf("premium"))

        expectThat(matchDto2.type).isEqualTo("BETWEEN")
        expectThat(matchDto2.operator).isEqualTo("BETWEEN")
        expectThat(matchDto2.valueType).isEqualTo("NUMBER")
        expectThat(matchDto2.values).isEqualTo(listOf(18, 65))

        expectThat(matchDto3.type).isEqualTo("CONTAINS")
        expectThat(matchDto3.operator).isEqualTo("CONTAINS")
        expectThat(matchDto3.valueType).isEqualTo("STRING")
        expectThat(matchDto3.values).isEqualTo(listOf("android", "mobile"))
    }

    @Test
    fun `TargetActionDto creation and properties`() {
        val targetAction1 = TargetActionDto(
            type = "VARIATION",
            variationId = 123L,
            bucketId = null
        )
        val targetAction2 = TargetActionDto(
            type = "BUCKET",
            variationId = null,
            bucketId = 456L
        )
        val targetAction3 = TargetActionDto(
            type = "DEFAULT",
            variationId = null,
            bucketId = null
        )

        expectThat(targetAction1.type).isEqualTo("VARIATION")
        expectThat(targetAction1.variationId).isEqualTo(123L)
        expectThat(targetAction1.bucketId).isEqualTo(null)

        expectThat(targetAction2.type).isEqualTo("BUCKET")
        expectThat(targetAction2.variationId).isEqualTo(null)
        expectThat(targetAction2.bucketId).isEqualTo(456L)

        expectThat(targetAction3.type).isEqualTo("DEFAULT")
        expectThat(targetAction3.variationId).isEqualTo(null)
        expectThat(targetAction3.bucketId).isEqualTo(null)
    }

    @Test
    fun `TargetRuleDto creation and properties`() {
        val keyDto = TargetDto.KeyDto(type = "DEVICE_TYPE", name = "device")
        val matchDto = TargetDto.MatchDto(type = "EQ", operator = "EQUALS", valueType = "STRING", values = listOf("mobile"))
        val conditionDto = TargetDto.ConditionDto(key = keyDto, match = matchDto)
        val targetDto = TargetDto(conditions = listOf(conditionDto))
        val actionDto = TargetActionDto(type = "VARIATION", variationId = 789L, bucketId = null)

        val targetRuleDto = TargetRuleDto(
            target = targetDto,
            action = actionDto
        )

        expectThat(targetRuleDto.target).isEqualTo(targetDto)
        expectThat(targetRuleDto.action).isEqualTo(actionDto)
    }

    @Test
    fun `TargetDto equals and hashCode`() {
        val keyDto = TargetDto.KeyDto(type = "USER_PROPERTY", name = "age")
        val matchDto = TargetDto.MatchDto(type = "GT", operator = "GREATER_THAN", valueType = "NUMBER", values = listOf(18))
        val conditionDto = TargetDto.ConditionDto(key = keyDto, match = matchDto)

        val target1 = TargetDto(conditions = listOf(conditionDto))
        val target2 = TargetDto(conditions = listOf(conditionDto))
        val target3 = TargetDto(conditions = emptyList())

        expectThat(target1).isEqualTo(target2)
        expectThat(target1.hashCode()).isEqualTo(target2.hashCode())
        expectThat(target1 == target3).isEqualTo(false)
    }

    @Test
    fun `ConditionDto equals and hashCode`() {
        val keyDto = TargetDto.KeyDto(type = "SEGMENT", name = "premium")
        val matchDto = TargetDto.MatchDto(type = "IN", operator = "IN", valueType = "STRING", values = listOf("value"))

        val condition1 = TargetDto.ConditionDto(key = keyDto, match = matchDto)
        val condition2 = TargetDto.ConditionDto(key = keyDto, match = matchDto)
        val condition3 = TargetDto.ConditionDto(
            key = TargetDto.KeyDto(type = "USER_ID", name = "id"),
            match = TargetDto.MatchDto(type = "EQ", operator = "EQUALS", valueType = "STRING", values = listOf("123"))
        )

        expectThat(condition1).isEqualTo(condition2)
        expectThat(condition1.hashCode()).isEqualTo(condition2.hashCode())
        expectThat(condition1 == condition3).isEqualTo(false)
    }

    @Test
    fun `KeyDto equals and hashCode`() {
        val key1 = TargetDto.KeyDto(type = "USER_PROPERTY", name = "subscription")
        val key2 = TargetDto.KeyDto(type = "USER_PROPERTY", name = "subscription")
        val key3 = TargetDto.KeyDto(type = "DEVICE_TYPE", name = "platform")

        expectThat(key1).isEqualTo(key2)
        expectThat(key1.hashCode()).isEqualTo(key2.hashCode())
        expectThat(key1 == key3).isEqualTo(false)
    }

    @Test
    fun `MatchDto equals and hashCode`() {
        val match1 = TargetDto.MatchDto(type = "GT", operator = "GREATER_THAN", valueType = "NUMBER", values = listOf(10))
        val match2 = TargetDto.MatchDto(type = "GT", operator = "GREATER_THAN", valueType = "NUMBER", values = listOf(10))
        val match3 = TargetDto.MatchDto(type = "LT", operator = "LESS_THAN", valueType = "NUMBER", values = listOf(5))

        expectThat(match1).isEqualTo(match2)
        expectThat(match1.hashCode()).isEqualTo(match2.hashCode())
        expectThat(match1 == match3).isEqualTo(false)
    }

    @Test
    fun `TargetActionDto equals and hashCode`() {
        val action1 = TargetActionDto(type = "VARIATION", variationId = 100L, bucketId = null)
        val action2 = TargetActionDto(type = "VARIATION", variationId = 100L, bucketId = null)
        val action3 = TargetActionDto(type = "BUCKET", variationId = null, bucketId = 200L)

        expectThat(action1).isEqualTo(action2)
        expectThat(action1.hashCode()).isEqualTo(action2.hashCode())
        expectThat(action1 == action3).isEqualTo(false)
    }

    @Test
    fun `TargetRuleDto equals and hashCode`() {
        val keyDto = TargetDto.KeyDto(type = "USER_ID", name = "userId")
        val matchDto = TargetDto.MatchDto(type = "EQ", operator = "EQUALS", valueType = "STRING", values = listOf("123"))
        val conditionDto = TargetDto.ConditionDto(key = keyDto, match = matchDto)
        val targetDto = TargetDto(conditions = listOf(conditionDto))
        val actionDto = TargetActionDto(type = "VARIATION", variationId = 100L, bucketId = null)

        val rule1 = TargetRuleDto(target = targetDto, action = actionDto)
        val rule2 = TargetRuleDto(target = targetDto, action = actionDto)
        val rule3 = TargetRuleDto(target = TargetDto(conditions = emptyList()), action = actionDto)

        expectThat(rule1).isEqualTo(rule2)
        expectThat(rule1.hashCode()).isEqualTo(rule2.hashCode())
        expectThat(rule1 == rule3).isEqualTo(false)
    }

    @Test
    fun `TargetDto copy method`() {
        val keyDto = TargetDto.KeyDto(type = "USER_PROPERTY", name = "age")
        val matchDto = TargetDto.MatchDto(type = "GT", operator = "GREATER_THAN", valueType = "NUMBER", values = listOf(18))
        val conditionDto = TargetDto.ConditionDto(key = keyDto, match = matchDto)

        val original = TargetDto(conditions = listOf(conditionDto))
        val copied = original.copy(conditions = emptyList())

        expectThat(copied.conditions).isEqualTo(emptyList())
        expectThat(original.conditions).isEqualTo(listOf(conditionDto))
    }

    @Test
    fun `ConditionDto copy method`() {
        val originalKey = TargetDto.KeyDto(type = "USER_PROPERTY", name = "original")
        val newKey = TargetDto.KeyDto(type = "SEGMENT", name = "modified")
        val matchDto = TargetDto.MatchDto(type = "EQ", operator = "EQUALS", valueType = "STRING", values = listOf("value"))

        val original = TargetDto.ConditionDto(key = originalKey, match = matchDto)
        val copied = original.copy(key = newKey)

        expectThat(copied.key).isEqualTo(newKey)
        expectThat(copied.match).isEqualTo(matchDto)
        expectThat(original.key).isEqualTo(originalKey)
    }

    @Test
    fun `TargetRuleDto copy method`() {
        val keyDto = TargetDto.KeyDto(type = "USER_ID", name = "userId")
        val matchDto = TargetDto.MatchDto(type = "EQ", operator = "EQUALS", valueType = "STRING", values = listOf("123"))
        val conditionDto = TargetDto.ConditionDto(key = keyDto, match = matchDto)
        val originalTarget = TargetDto(conditions = listOf(conditionDto))
        val newTarget = TargetDto(conditions = emptyList())
        val actionDto = TargetActionDto(type = "VARIATION", variationId = 100L, bucketId = null)

        val original = TargetRuleDto(target = originalTarget, action = actionDto)
        val copied = original.copy(target = newTarget)

        expectThat(copied.target).isEqualTo(newTarget)
        expectThat(copied.action).isEqualTo(actionDto)
        expectThat(original.target).isEqualTo(originalTarget)
    }
}