package io.hackle.android.internal.properties

import io.hackle.sdk.common.PropertiesBuilder
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PropertyOperatorTest {

    private fun PropertyOperator.verify(
        base: Map<String, Any>,
        properties: Map<String, Any>,
        expected: Map<String, Any>,
    ) {
        expectThat(operate(base, properties)) isEqualTo expected
    }

    fun properties(builder: PropertiesBuilder.() -> Unit): Map<String, Any> {
        return PropertiesBuilder().apply(builder).build()
    }

    @Test
    fun `PropertySetOperator`() {
        val p1 = properties {
            add("name", "Yong")
            add("location", "Seoul")
            add("job", "Server Developer")
        }

        val p2 = properties {
            add("job", "SDK Developer")
            add("company", "Hackle")
        }

        PropertySetOperator.verify(emptyMap(), p1, p1)
        PropertySetOperator.verify(p1, emptyMap(), p1)
        PropertySetOperator.verify(p1, p2, properties {
            add("name", "Yong")
            add("location", "Seoul")
            add("job", "SDK Developer")
            add("company", "Hackle")
        })
    }

    @Test
    fun `PropertySetOnceOperator`() {
        val p1 = properties {
            add("name", "Yong")
            add("location", "Seoul")
            add("job", "Server Developer")
        }

        val p2 = properties {
            add("job", "SDK Developer")
            add("company", "Hackle")
        }

        PropertySetOnceOperator.verify(emptyMap(), p1, p1)
        PropertySetOnceOperator.verify(p1, emptyMap(), p1)
        PropertySetOnceOperator.verify(p1, p2, properties {
            add("name", "Yong")
            add("location", "Seoul")
            add("job", "Server Developer")
            add("company", "Hackle")
        })
    }

    @Test
    fun `PropertyUnsetOperator`() {
        val p1 = properties {
            add("name", "Yong")
            add("location", "Seoul")
            add("job", "Server Developer")
        }

        val p2 = properties {
            add("job", "SDK Developer")
            add("company", "Hackle")
        }

        PropertyUnsetOperator.verify(emptyMap(), p1, emptyMap())
        PropertyUnsetOperator.verify(p1, emptyMap(), p1)
        PropertyUnsetOperator.verify(p1, p2, properties {
            add("name", "Yong")
            add("location", "Seoul")
        })
    }

    @Test
    fun `PropertyIncrementOperator`() {
        fun verify(base: Any?, value: Any?, expected: Any?) {
            PropertyIncrementOperator.verify(
                if (base != null) properties { add("number", base) } else emptyMap(),
                if (value != null) properties { add("number", value) } else emptyMap(),
                if (expected != null) properties { add("number", expected) } else emptyMap(),
            )
        }

        verify(null, null, null)
        verify(null, 42, 42)
        verify(null, "42", null)

        verify(42, null, 42)
        verify(42, 42, 84.0)
        verify(42, "42", 42)

        verify("42", null, "42")
        verify("42", 42, "42")
        verify("42", "42", "42")
    }

    private inner class ArrayOperatorAssertion(private val operator: ArrayPropertyOperator) {

        infix fun Any?.operate(value: Any?): Map<String, Any> {
            val base = this
            return operator.operate(
                if (base != null) properties { add("arr", base) } else emptyMap(),
                if (value != null) properties { add("arr", value) } else emptyMap(),
            )
        }

        infix fun Map<String, Any>.isEquals(other: Any?) {
            val properties = if (other != null) properties { add("arr", other) } else emptyMap()
            expectThat(this) isEqualTo properties
        }
    }

    private fun assert(operator: ArrayPropertyOperator, assert: ArrayOperatorAssertion.() -> Unit) {
        ArrayOperatorAssertion(operator).apply(assert)
    }


    @Test
    fun `PropertyAppendOperator`() {
        assert(PropertyAppendOperator) {
            null operate null isEquals null
            null operate 1 isEquals listOf(1)
            null operate listOf(1) isEquals listOf(1)
            null operate listOf(1, 2, 3) isEquals listOf(1, 2, 3)
            null operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3, 1, 2)

            1 operate null isEquals 1
            1 operate 1 isEquals listOf(1, 1)
            1 operate listOf(1) isEquals listOf(1, 1)
            1 operate listOf(1, 2, 3) isEquals listOf(1, 1, 2, 3)
            1 operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 1, 2, 3, 1, 2)

            2 operate null isEquals 2
            2 operate 1 isEquals listOf(2, 1)
            2 operate listOf(1) isEquals listOf(2, 1)
            2 operate listOf(1, 2, 3) isEquals listOf(2, 1, 2, 3)
            2 operate listOf(1, 2, 3, 1, 2) isEquals listOf(2, 1, 2, 3, 1, 2)

            listOf(1) operate null isEquals listOf(1)
            listOf(1) operate 1 isEquals listOf(1, 1)
            listOf(1) operate listOf(1) isEquals listOf(1, 1)
            listOf(1) operate listOf(1, 2, 3) isEquals listOf(1, 1, 2, 3)
            listOf(1) operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 1, 2, 3, 1, 2)

            listOf(1, 3, 5) operate null isEquals listOf(1, 3, 5)
            listOf(1, 3, 5) operate 1 isEquals listOf(1, 3, 5, 1)
            listOf(1, 3, 5) operate listOf(1) isEquals listOf(1, 3, 5, 1)
            listOf(1, 3, 5) operate listOf(1, 2, 3) isEquals listOf(1, 3, 5, 1, 2, 3)
            listOf(1, 3, 5) operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 3, 5, 1, 2, 3, 1, 2)
        }
    }

    @Test
    fun `PropertyAppendOnceOperatorTest`() {
        assert(PropertyAppendOnceOperator) {
            null operate 1 isEquals listOf(1)
            null operate listOf(1) isEquals listOf(1)
            null operate listOf(1, 2, 3) isEquals listOf(1, 2, 3)
            null operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3)

            1 operate 1 isEquals listOf(1)
            1 operate listOf(1) isEquals listOf(1)
            1 operate listOf(1, 2, 3) isEquals listOf(1, 2, 3)
            1 operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3)

            2 operate 1 isEquals listOf(2, 1)
            2 operate listOf(1) isEquals listOf(2, 1)
            2 operate listOf(1, 2, 3) isEquals listOf(2, 1, 3)
            2 operate listOf(1, 2, 3, 1, 2) isEquals listOf(2, 1, 3)

            listOf(1) operate 1 isEquals listOf(1)
            listOf(1) operate listOf(1) isEquals listOf(1)
            listOf(1) operate listOf(1, 2, 3) isEquals listOf(1, 2, 3)
            listOf(1) operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3)

            listOf(1, 3, 5) operate 1 isEquals listOf(1, 3, 5)
            listOf(1, 3, 5) operate listOf(1) isEquals listOf(1, 3, 5)
            listOf(1, 3, 5) operate listOf(1, 2, 3) isEquals listOf(1, 3, 5, 2)
            listOf(1, 3, 5) operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 3, 5, 2)
        }
    }

    @Test
    fun `PropertyPrependOperatorTest`() {
        assert(PropertyPrependOperator) {
            null operate 1 isEquals listOf(1)
            null operate listOf(1) isEquals listOf(1)
            null operate listOf(1, 2, 3) isEquals listOf(1, 2, 3)
            null operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3, 1, 2)

            1 operate 1 isEquals listOf(1, 1)
            1 operate listOf(1) isEquals listOf(1, 1)
            1 operate listOf(1, 2, 3) isEquals listOf(1, 2, 3, 1)
            1 operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3, 1, 2, 1)

            2 operate 1 isEquals listOf(1, 2)
            2 operate listOf(1) isEquals listOf(1, 2)
            2 operate listOf(1, 2, 3) isEquals listOf(1, 2, 3, 2)
            2 operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3, 1, 2, 2)

            listOf(1) operate 1 isEquals listOf(1, 1)
            listOf(1) operate listOf(1) isEquals listOf(1, 1)
            listOf(1) operate listOf(1, 2, 3) isEquals listOf(1, 2, 3, 1)
            listOf(1) operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3, 1, 2, 1)

            listOf(1, 3, 5) operate 1 isEquals listOf(1, 1, 3, 5)
            listOf(1, 3, 5) operate listOf(1) isEquals listOf(1, 1, 3, 5)
            listOf(1, 3, 5) operate listOf(1, 2, 3) isEquals listOf(1, 2, 3, 1, 3, 5)
            listOf(1, 3, 5) operate listOf(1, 2, 3, 1, 2) isEquals listOf(1,
                2,
                3,
                1,
                2,
                1,
                3,
                5)
        }
    }

    @Test
    fun `PropertyPrependOnceOperatorTest`() {
        assert(PropertyPrependOnceOperator) {
            null operate 1 isEquals listOf(1)
            null operate listOf(1) isEquals listOf(1)
            null operate listOf(1, 2, 3) isEquals listOf(1, 2, 3)
            null operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 2, 3)

            1 operate 1 isEquals listOf(1)
            1 operate listOf(1) isEquals listOf(1)
            1 operate listOf(1, 2, 3) isEquals listOf(2, 3, 1)
            1 operate listOf(1, 2, 3, 1, 2) isEquals listOf(2, 3, 1)

            2 operate 1 isEquals listOf(1, 2)
            2 operate listOf(1) isEquals listOf(1, 2)
            2 operate listOf(1, 2, 3) isEquals listOf(1, 3, 2)
            2 operate listOf(1, 2, 3, 1, 2) isEquals listOf(1, 3, 2)

            listOf(1) operate 1 isEquals listOf(1)
            listOf(1) operate listOf(1) isEquals listOf(1)
            listOf(1) operate listOf(1, 2, 3) isEquals listOf(2, 3, 1)
            listOf(1) operate listOf(1, 2, 3, 1, 2) isEquals listOf(2, 3, 1)

            listOf(1, 3, 5) operate 1 isEquals listOf(1, 3, 5)
            listOf(1, 3, 5) operate listOf(1) isEquals listOf(1, 3, 5)
            listOf(1, 3, 5) operate listOf(1, 2, 3) isEquals listOf(2, 1, 3, 5)
            listOf(1, 3, 5) operate listOf(1, 2, 3, 1, 2) isEquals listOf(2, 1, 3, 5)
        }
    }

    @Test
    fun `PropertyPropertyRemoveOperatorTest`() {
        assert(PropertyRemoveOperator) {
            null operate 1 isEquals listOf<Int>()
            null operate listOf(1) isEquals listOf<Int>()
            null operate listOf(1, 2, 3) isEquals listOf<Int>()
            null operate listOf(1, 2, 3, 1, 2) isEquals listOf<Int>()

            1 operate 1 isEquals listOf<Int>()
            1 operate listOf(1) isEquals listOf<Int>()
            1 operate listOf(1, 2, 3) isEquals listOf<Int>()
            1 operate listOf(1, 2, 3, 1, 2) isEquals listOf<Int>()

            2 operate 1 isEquals listOf(2)
            2 operate listOf(1) isEquals listOf(2)
            2 operate listOf(1, 2, 3) isEquals listOf<Int>()
            2 operate listOf(1, 2, 3, 1, 2) isEquals listOf<Int>()

            listOf(1) operate 1 isEquals listOf<Int>()
            listOf(1) operate listOf(1) isEquals listOf<Int>()
            listOf(1) operate listOf(1, 2, 3) isEquals listOf<Int>()
            listOf(1) operate listOf(1, 2, 3, 1, 2) isEquals listOf<Int>()

            listOf(1, 3, 5) operate 1 isEquals listOf(3, 5)
            listOf(1, 3, 5) operate listOf(1) isEquals listOf(3, 5)
            listOf(1, 3, 5) operate listOf(1, 2, 3) isEquals listOf(5)
            listOf(1, 3, 5) operate listOf(1, 2, 3, 1, 2) isEquals listOf(5)

            listOf(1, 2, 3, 2, 1) operate listOf(1) isEquals listOf(2, 3, 2)
        }
    }

    @Test
    fun `PropertyClearAllOperator`() {
        PropertyClearAllOperator.verify(properties { add("age", 30) }, emptyMap(), emptyMap())
    }
}