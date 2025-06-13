package io.hackle.android.internal.user

import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.TargetEvent
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class UserTargetEventsTest {
    @Test
    fun from() {
        expectThat(UserTargetEvents.from(emptyList())).isEqualTo(UserTargetEvents.empty())

        val targetEventList = listOf(
            TargetEvent(
                "purchase",
                listOf(
                    TargetEvent.Stat(1737361789000, 10),
                    TargetEvent.Stat(1737361790000, 20),
                    TargetEvent.Stat(1737361793000, 30)
                ),
                TargetEvent.Property(
                    "product_name",
                    Target.Key.Type.EVENT_PROPERTY,
                    "shampoo"
                )
            )
        )
        val targetEvents = UserTargetEvents(targetEventList)
        expectThat(targetEvents.asList()).isEqualTo(targetEventList)
    }

    @Test
    fun userTargetEvent() {
        val targetEvent = TargetEvent(
            "purchase",
            listOf(
                TargetEvent.Stat(1737361789000, 10),
                TargetEvent.Stat(1737361790000, 20),
                TargetEvent.Stat(1737361793000, 30)
            ),
            TargetEvent.Property(
                "product_name",
                Target.Key.Type.EVENT_PROPERTY,
                "shampoo"
            )
        )
        expectThat(targetEvent) {
            get { eventKey }.isEqualTo("purchase")
            get { stats }.isEqualTo(
                listOf(
                    TargetEvent.Stat(1737361789000, 10),
                    TargetEvent.Stat(1737361790000, 20),
                    TargetEvent.Stat(1737361793000, 30)
                )
            )
            get { property }.isEqualTo(
                TargetEvent.Property(
                    "product_name",
                    Target.Key.Type.EVENT_PROPERTY,
                    "shampoo"
                )
            )
        }
    }

    @Test
    fun `toStat - TargetEventStatDto를 TargetEvent_Stat으로 올바르게 변환한다`() {
        // given
        val statDto = TargetEventStatDto(date = 20250613L, count = 42)

        // when
        val stat = statDto.toStat()

        // then
        expectThat(stat) {
            get { date }.isEqualTo(20250613L)
            get { count }.isEqualTo(42)
        }
    }

    @Test
    fun `toProperty - 유효한 타입의 TargetEventPropertyDto를 TargetEvent_Property로 올바르게 변환한다`() {
        // given
        val propertyDto = TargetEventPropertyDto(key = "user_name", type = "EVENT_PROPERTY", value = "hackle")

        // when
        val property = propertyDto.toProperty()

        // then
        expectThat(property).isNotNull().and {
            get { key }.isEqualTo("user_name")
            get { type }.isEqualTo(Target.Key.Type.EVENT_PROPERTY)
            get { value }.isEqualTo("hackle")
        }
    }

    @Test
    fun `toProperty - 유효하지 않은 타입의 TargetEventPropertyDto는 null을 반환한다`() {
        // given
        val propertyDto = TargetEventPropertyDto(key = "invalid_prop", type = "unknown_type", value = 123)

        // when
        val property = propertyDto.toProperty()

        // then
        expectThat(property).isNull()
    }

    @Test
    fun `toTargetEvent - 모든 필드가 포함된 TargetEventDto를 TargetEvent로 올바르게 변환한다`() {
        // given
        val dto = TargetEventDto(
            eventKey = "purchase",
            stats = listOf(TargetEventStatDto(date = 20250613L, count = 1)),
            property = TargetEventPropertyDto(key = "price", type = "EVENT_PROPERTY", value = 50000)
        )

        // when
        val targetEvent = dto.toTargetEvent()

        // then
        expectThat(targetEvent) {
            get { eventKey }.isEqualTo("purchase")
            get { stats }.hasSize(1)
            get { stats.first().count }.isEqualTo(1)
            get { property }.isNotNull().and {
                get { key }.isEqualTo("price")
                get { type }.isEqualTo(Target.Key.Type.EVENT_PROPERTY)
                get { value }.isEqualTo(50000)
            }
        }
    }

    @Test
    fun `toTargetEvent - property가 null인 TargetEventDto도 올바르게 변환한다`() {
        // given
        val dto = TargetEventDto(
            eventKey = "login",
            stats = listOf(TargetEventStatDto(date = 20250613L, count = 5)),
            property = null
        )

        // when
        val targetEvent = dto.toTargetEvent()

        // then
        expectThat(targetEvent) {
            get { eventKey }.isEqualTo("login")
            get { stats }.hasSize(1)
            get { property }.isNull()
        }
    }

    @Test
    fun `toTargetEvent - stats 리스트가 비어있는 TargetEventDto도 올바르게 변환한다`() {
        // given
        val dto = TargetEventDto(
            eventKey = "app_open",
            stats = emptyList(),
            property = null
        )

        // when
        val targetEvent = dto.toTargetEvent()

        // then
        expectThat(targetEvent) {
            get { eventKey }.isEqualTo("app_open")
            get { stats }.isEmpty()
            get { property }.isNull()
        }
    }
}
