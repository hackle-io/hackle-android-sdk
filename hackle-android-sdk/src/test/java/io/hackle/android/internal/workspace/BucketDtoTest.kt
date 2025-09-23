package io.hackle.android.internal.workspace

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class BucketDtoTest {

    @Test
    fun `BucketDto creation and properties`() {
        val slotDto1 = SlotDto(
            startInclusive = 0,
            endExclusive = 5000,
            variationId = 100L
        )
        val slotDto2 = SlotDto(
            startInclusive = 5000,
            endExclusive = 10000,
            variationId = 200L
        )
        val bucketDto = BucketDto(
            id = 1L,
            seed = 12345,
            slotSize = 10000,
            slots = listOf(slotDto1, slotDto2)
        )

        expectThat(bucketDto.id).isEqualTo(1L)
        expectThat(bucketDto.seed).isEqualTo(12345)
        expectThat(bucketDto.slotSize).isEqualTo(10000)
        expectThat(bucketDto.slots).isEqualTo(listOf(slotDto1, slotDto2))
    }

    @Test
    fun `SlotDto creation and properties`() {
        val slotDto1 = SlotDto(
            startInclusive = 1000,
            endExclusive = 2000,
            variationId = 500L
        )
        val slotDto2 = SlotDto(
            startInclusive = 2000,
            endExclusive = 3000,
            variationId = 600L
        )

        expectThat(slotDto1.startInclusive).isEqualTo(1000)
        expectThat(slotDto1.endExclusive).isEqualTo(2000)
        expectThat(slotDto1.variationId).isEqualTo(500L)

        expectThat(slotDto2.startInclusive).isEqualTo(2000)
        expectThat(slotDto2.endExclusive).isEqualTo(3000)
        expectThat(slotDto2.variationId).isEqualTo(600L)
    }

    @Test
    fun `EventTypeDto creation and properties`() {
        val eventTypeDto1 = EventTypeDto(
            id = 10L,
            key = "page_view"
        )
        val eventTypeDto2 = EventTypeDto(
            id = 20L,
            key = "button_click"
        )
        val eventTypeDto3 = EventTypeDto(
            id = 30L,
            key = "purchase"
        )

        expectThat(eventTypeDto1.id).isEqualTo(10L)
        expectThat(eventTypeDto1.key).isEqualTo("page_view")

        expectThat(eventTypeDto2.id).isEqualTo(20L)
        expectThat(eventTypeDto2.key).isEqualTo("button_click")

        expectThat(eventTypeDto3.id).isEqualTo(30L)
        expectThat(eventTypeDto3.key).isEqualTo("purchase")
    }

    @Test
    fun `SegmentDto creation and properties`() {
        val targetDto = TargetDto(
            conditions = emptyList()
        )
        val segmentDto = SegmentDto(
            id = 100L,
            key = "premium_users",
            type = "USER_SEGMENT",
            targets = listOf(targetDto)
        )

        expectThat(segmentDto.id).isEqualTo(100L)
        expectThat(segmentDto.key).isEqualTo("premium_users")
        expectThat(segmentDto.type).isEqualTo("USER_SEGMENT")
        expectThat(segmentDto.targets).isEqualTo(listOf(targetDto))
    }

    @Test
    fun `BucketDto equals and hashCode`() {
        val slotDto = SlotDto(startInclusive = 0, endExclusive = 1000, variationId = 100L)

        val bucket1 = BucketDto(
            id = 1L,
            seed = 12345,
            slotSize = 10000,
            slots = listOf(slotDto)
        )
        val bucket2 = BucketDto(
            id = 1L,
            seed = 12345,
            slotSize = 10000,
            slots = listOf(slotDto)
        )
        val bucket3 = BucketDto(
            id = 2L,
            seed = 67890,
            slotSize = 5000,
            slots = emptyList()
        )

        expectThat(bucket1).isEqualTo(bucket2)
        expectThat(bucket1.hashCode()).isEqualTo(bucket2.hashCode())
        expectThat(bucket1 == bucket3).isEqualTo(false)
    }

    @Test
    fun `SlotDto equals and hashCode`() {
        val slot1 = SlotDto(
            startInclusive = 1000,
            endExclusive = 2000,
            variationId = 100L
        )
        val slot2 = SlotDto(
            startInclusive = 1000,
            endExclusive = 2000,
            variationId = 100L
        )
        val slot3 = SlotDto(
            startInclusive = 2000,
            endExclusive = 3000,
            variationId = 200L
        )

        expectThat(slot1).isEqualTo(slot2)
        expectThat(slot1.hashCode()).isEqualTo(slot2.hashCode())
        expectThat(slot1 == slot3).isEqualTo(false)
    }

    @Test
    fun `EventTypeDto equals and hashCode`() {
        val event1 = EventTypeDto(
            id = 10L,
            key = "page_view"
        )
        val event2 = EventTypeDto(
            id = 10L,
            key = "page_view"
        )
        val event3 = EventTypeDto(
            id = 20L,
            key = "button_click"
        )

        expectThat(event1).isEqualTo(event2)
        expectThat(event1.hashCode()).isEqualTo(event2.hashCode())
        expectThat(event1 == event3).isEqualTo(false)
    }

    @Test
    fun `SegmentDto equals and hashCode`() {
        val targetDto = TargetDto(conditions = emptyList())

        val segment1 = SegmentDto(
            id = 100L,
            key = "premium",
            type = "USER_SEGMENT",
            targets = listOf(targetDto)
        )
        val segment2 = SegmentDto(
            id = 100L,
            key = "premium",
            type = "USER_SEGMENT",
            targets = listOf(targetDto)
        )
        val segment3 = SegmentDto(
            id = 200L,
            key = "basic",
            type = "DEVICE_SEGMENT",
            targets = emptyList()
        )

        expectThat(segment1).isEqualTo(segment2)
        expectThat(segment1.hashCode()).isEqualTo(segment2.hashCode())
        expectThat(segment1 == segment3).isEqualTo(false)
    }

    @Test
    fun `BucketDto copy method`() {
        val originalSlot = SlotDto(startInclusive = 0, endExclusive = 1000, variationId = 100L)
        val newSlot = SlotDto(startInclusive = 1000, endExclusive = 2000, variationId = 200L)

        val original = BucketDto(
            id = 1L,
            seed = 12345,
            slotSize = 10000,
            slots = listOf(originalSlot)
        )
        val copied = original.copy(
            seed = 67890,
            slots = listOf(newSlot)
        )

        expectThat(copied.seed).isEqualTo(67890)
        expectThat(copied.slots).isEqualTo(listOf(newSlot))
        expectThat(copied.id).isEqualTo(1L)
        expectThat(copied.slotSize).isEqualTo(10000)
        expectThat(original.seed).isEqualTo(12345)
        expectThat(original.slots).isEqualTo(listOf(originalSlot))
    }

    @Test
    fun `SlotDto copy method`() {
        val original = SlotDto(
            startInclusive = 1000,
            endExclusive = 2000,
            variationId = 100L
        )
        val copied = original.copy(
            startInclusive = 2000,
            endExclusive = 3000
        )

        expectThat(copied.startInclusive).isEqualTo(2000)
        expectThat(copied.endExclusive).isEqualTo(3000)
        expectThat(copied.variationId).isEqualTo(100L)
        expectThat(original.startInclusive).isEqualTo(1000)
        expectThat(original.endExclusive).isEqualTo(2000)
    }

    @Test
    fun `EventTypeDto copy method`() {
        val original = EventTypeDto(
            id = 10L,
            key = "original_key"
        )
        val copied = original.copy(
            key = "modified_key"
        )

        expectThat(copied.key).isEqualTo("modified_key")
        expectThat(copied.id).isEqualTo(10L)
        expectThat(original.key).isEqualTo("original_key")
    }

    @Test
    fun `SegmentDto copy method`() {
        val originalTarget = TargetDto(conditions = emptyList())
        val newTarget = TargetDto(conditions = emptyList())

        val original = SegmentDto(
            id = 100L,
            key = "original_segment",
            type = "USER_SEGMENT",
            targets = listOf(originalTarget)
        )
        val copied = original.copy(
            key = "modified_segment",
            type = "DEVICE_SEGMENT",
            targets = listOf(newTarget)
        )

        expectThat(copied.key).isEqualTo("modified_segment")
        expectThat(copied.type).isEqualTo("DEVICE_SEGMENT")
        expectThat(copied.targets).isEqualTo(listOf(newTarget))
        expectThat(copied.id).isEqualTo(100L)
        expectThat(original.key).isEqualTo("original_segment")
        expectThat(original.type).isEqualTo("USER_SEGMENT")
        expectThat(original.targets).isEqualTo(listOf(originalTarget))
    }

    @Test
    fun `BucketDto with empty slots`() {
        val bucketDto = BucketDto(
            id = 99L,
            seed = 0,
            slotSize = 1000,
            slots = emptyList()
        )

        expectThat(bucketDto.id).isEqualTo(99L)
        expectThat(bucketDto.seed).isEqualTo(0)
        expectThat(bucketDto.slotSize).isEqualTo(1000)
        expectThat(bucketDto.slots).isEqualTo(emptyList())
    }

    @Test
    fun `SegmentDto with empty targets`() {
        val segmentDto = SegmentDto(
            id = 999L,
            key = "empty_segment",
            type = "CUSTOM_SEGMENT",
            targets = emptyList()
        )

        expectThat(segmentDto.id).isEqualTo(999L)
        expectThat(segmentDto.key).isEqualTo("empty_segment")
        expectThat(segmentDto.type).isEqualTo("CUSTOM_SEGMENT")
        expectThat(segmentDto.targets).isEqualTo(emptyList())
    }
}