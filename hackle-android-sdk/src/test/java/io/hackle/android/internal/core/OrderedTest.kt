package io.hackle.android.internal.core

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class OrderedTest {

    @Test
    fun `order`() {
        expectThat(Ordered.HIGHEST).isEqualTo(Int.MIN_VALUE)
        expectThat(Ordered.MEDIUM).isEqualTo(0)
        expectThat(Ordered.LOWEST).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `SimpleOrdered`() {
        expectThat(SimpleOrdered.of(1, 1).compareTo(SimpleOrdered.of(1, 1))).isEqualTo(0)
        expectThat(SimpleOrdered.of(1, 1).compareTo(SimpleOrdered.of(1, 2))).isEqualTo(-1)
        expectThat(SimpleOrdered.of(1, 1).compareTo(SimpleOrdered.of(1, 0))).isEqualTo(1)
    }
}
