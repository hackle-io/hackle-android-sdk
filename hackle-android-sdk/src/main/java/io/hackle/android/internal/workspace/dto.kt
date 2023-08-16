package io.hackle.android.internal.workspace

internal data class WorkspaceDto(
    val experiments: List<ExperimentDto>,
    val featureFlags: List<ExperimentDto>,
    val buckets: List<BucketDto>,
    val events: List<EventTypeDto>,
    val segments: List<SegmentDto>,
    val containers: List<ContainerDto>,
    val parameterConfigurations: List<ParameterConfigurationDto>,
    val remoteConfigParameters: List<RemoteConfigParameterDto>,
    val inAppMessages: List<InAppMessageDto>
)

internal data class ExperimentDto(
    val id: Long,
    val key: Long,
    val name: String?,
    val status: String,
    val version: Int,
    val variations: List<VariationDto>,
    val execution: ExecutionDto,
    val winnerVariationId: Long?,
    val identifierType: String,
    val containerId: Long?,
)

internal data class VariationDto(
    val id: Long,
    val key: String,
    val status: String,
    val parameterConfigurationId: Long?,
)

internal data class ExecutionDto(
    val status: String,
    val version: Int,
    val userOverrides: List<UserOverrideDto>,
    val segmentOverrides: List<TargetRuleDto>,
    val targetAudiences: List<TargetDto>,
    val targetRules: List<TargetRuleDto>,
    val defaultRule: TargetActionDto,
)

internal data class UserOverrideDto(
    val userId: String,
    val variationId: Long,
)

internal data class BucketDto(
    val id: Long,
    val seed: Int,
    val slotSize: Int,
    val slots: List<SlotDto>,
)

internal data class SlotDto(
    val startInclusive: Int,
    val endExclusive: Int,
    val variationId: Long,
)

internal data class EventTypeDto(
    val id: Long,
    val key: String,
)

internal data class TargetDto(
    val conditions: List<ConditionDto>,
) {
    data class ConditionDto(
        val key: KeyDto,
        val match: MatchDto,
    )

    data class KeyDto(
        val type: String,
        val name: String,
    )

    data class MatchDto(
        val type: String,
        val operator: String,
        val valueType: String,
        val values: List<Any>,
    )
}

internal data class TargetActionDto(
    val type: String,
    val variationId: Long?,
    val bucketId: Long?,
)

internal data class TargetRuleDto(
    val target: TargetDto,
    val action: TargetActionDto,
)

internal data class SegmentDto(
    val id: Long,
    val key: String,
    val type: String,
    val targets: List<TargetDto>,
)

internal data class ContainerDto(
    val id: Long,
    val environmentId: Long,
    val bucketId: Long,
    val groups: List<ContainerGroupDto>,
)

internal data class ContainerGroupDto(
    val id: Long,
    val experiments: List<Long>,
)

internal data class ParameterConfigurationDto(
    val id: Long,
    val parameters: List<ParameterDto>,
) {
    data class ParameterDto(
        val key: String,
        val value: Any,
    )
}

internal data class RemoteConfigParameterDto(
    val id: Long,
    val key: String,
    val type: String,
    val identifierType: String,
    val targetRules: List<TargetRuleDto>,
    val defaultValue: ValueDto,
) {

    data class TargetRuleDto(
        val key: String,
        val name: String,
        val target: TargetDto,
        val bucketId: Long,
        val value: ValueDto,
    )

    data class ValueDto(
        val id: Long,
        val value: Any,
    )
}

internal class DurationDto(
    val timeUnit: String,
    val amount: Long
)

internal data class InAppMessageDto(
    val id: Long,
    val key: Long,
    val timeUnit: String,
    val startEpochTimeMillis: Long?,
    val endEpochTimeMillis: Long?,
    val status: String,
    val eventTriggerRules: List<EventTriggerRuleDto>,
    val eventFrequencyCap: EventFrequencyCapDto?,
    val targetContext: TargetContextDto,
    val messageContext: MessageContextDto
) {

    data class EventTriggerRuleDto(
        val eventKey: String,
        val targets: List<TargetDto>
    )

    data class EventFrequencyCapDto(
        val identifiers: List<IdentifierCapDto>,
        val duration: DurationCapDto?
    )

    data class IdentifierCapDto(
        val identifierType: String,
        val countPerIdentifier: Long
    )

    data class DurationCapDto(
        val durationUnit: DurationDto,
        val countPerDuration: Long
    )

    data class TargetContextDto(
        val targets: List<TargetDto>,
        val overrides: List<UserOverrideDto>
    ) {
        data class UserOverrideDto(
            val identifierType: String,
            val identifiers: List<String>
        )
    }

    data class MessageContextDto(
        val defaultLang: String,
        val platformTypes: List<String>,
        val orientations: List<String>,
        val exposure: MessageDto.ExposureDto,
        val messages: List<MessageDto>
    ) {
        data class MessageDto(
            val lang: String,
            val layout: LayoutDto,
            val images: List<ImageDto>,
            val text: TextDto?,
            val buttons: List<ButtonDto>,
            val background: BackgroundDto,
            val closeButton: CloseButtonDto?
        ) {

            data class LayoutDto(
                val displayType: String,
                val layoutType: String
            )

            data class ImageDto(
                val orientation: String,
                val imagePath: String,
                val action: ActionDto?
            )

            data class TextDto(
                val title: TextAttributeDto,
                val body: TextAttributeDto
            ) {
                data class TextAttributeDto(
                    val text: String,
                    val style: StyleDto
                )

                data class StyleDto(
                    val textColor: String
                )
            }

            data class ButtonDto(
                val text: String,
                val style: StyleDto,
                val action: ActionDto
            ) {

                data class StyleDto(
                    val textColor: String,
                    val bgColor: String,
                    val borderColor: String
                )
            }

            data class BackgroundDto(
                val color: String
            )

            data class CloseButtonDto(
                val style: StyleDto,
                val action: ActionDto
            ) {
                data class StyleDto(
                    val color: String
                )
            }

            data class ExposureDto(
                val type: String,
                val key: Long?
            )
        }

        data class ActionDto(
            val behavior: String,
            val type: String,
            val value: String?
        )
    }
}