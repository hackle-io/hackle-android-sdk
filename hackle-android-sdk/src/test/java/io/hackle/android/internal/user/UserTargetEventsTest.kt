package io.hackle.android.internal.user

import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.TargetEvent
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

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
}