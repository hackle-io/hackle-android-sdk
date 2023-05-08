package io.hackle.android.internal.properties

import io.hackle.sdk.common.PropertiesBuilder
import java.util.*

internal interface PropertyOperator {
    fun operate(base: Map<String, Any>, properties: Map<String, Any>): Map<String, Any>
}

internal object PropertySetOperator : PropertyOperator {
    override fun operate(base: Map<String, Any>, properties: Map<String, Any>): Map<String, Any> {
        return when {
            base.isEmpty() -> properties
            properties.isEmpty() -> base
            else -> PropertiesBuilder().add(base).add(properties).build()
        }
    }
}

internal object PropertySetOnceOperator : PropertyOperator {
    override fun operate(base: Map<String, Any>, properties: Map<String, Any>): Map<String, Any> {
        return when {
            base.isEmpty() -> properties
            properties.isEmpty() -> base
            else -> PropertiesBuilder().add(base).add(properties, setOnce = true).build()
        }
    }
}

internal object PropertyUnsetOperator : PropertyOperator {
    override fun operate(base: Map<String, Any>, properties: Map<String, Any>): Map<String, Any> {
        return when {
            base.isEmpty() -> emptyMap()
            properties.isEmpty() -> base
            else -> PropertiesBuilder().add(base).remove(properties).build()
        }
    }
}

internal object PropertyIncrementOperator : PropertyOperator {
    override fun operate(base: Map<String, Any>, properties: Map<String, Any>): Map<String, Any> {
        if (properties.isEmpty()) {
            return base
        }

        val builder = PropertiesBuilder().add(base)
        for ((key, value) in properties) {
            builder.compute(key) { operate(it, value) }
        }
        return builder.build()
    }

    private fun operate(baseValue: Any?, valueToIncrement: Any): Any? {
        return when {
            valueToIncrement !is Number -> baseValue
            baseValue == null -> valueToIncrement
            baseValue !is Number -> baseValue
            else -> baseValue.toDouble() + valueToIncrement.toDouble()
        }
    }
}

internal abstract class ArrayPropertyOperator : PropertyOperator {
    final override fun operate(
        base: Map<String, Any>,
        properties: Map<String, Any>,
    ): Map<String, Any> {
        if (properties.isEmpty()) {
            return base
        }

        val builder = PropertiesBuilder().add(base)
        for ((key, value) in properties) {
            builder.compute(key) { baseValue -> compute(baseValue, value) }
        }
        return builder.build()
    }

    private fun compute(baseValue: Any?, valueToOperate: Any): List<Any> {
        val base = baseValue?.let(this::toList)?.toMutableList() ?: arrayListOf()
        val values = toList(valueToOperate)
        val operatedArray = operate(base, values)
        return Collections.unmodifiableList(operatedArray)
    }

    private fun toList(value: Any): List<Any> {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is List<*> -> value as List<Any>
            else -> listOf(value)
        }
    }

    protected fun append(
        base: MutableList<Any>,
        value: Any,
        setOnce: Boolean = false,
    ): MutableList<Any> {
        if (setOnce && contains(base, value)) {
            return base
        }
        base.add(value)
        return base
    }

    protected fun prepend(
        value: Any,
        base: MutableList<Any>,
        setOnce: Boolean = false,
    ): MutableList<Any> {
        if (setOnce && contains(base, value)) {
            return base
        }
        base.add(0, value)
        return base
    }


    private fun contains(base: MutableList<Any>, value: Any): Boolean {
        return base.any { PropertyOperators.equals(it, value) }
    }

    protected abstract fun operate(base: MutableList<Any>, values: List<Any>): List<Any>
}


internal object PropertyAppendOperator : ArrayPropertyOperator() {
    override fun operate(base: MutableList<Any>, values: List<Any>): List<Any> {
        return values.fold(base, this::append)
    }
}

internal object PropertyAppendOnceOperator : ArrayPropertyOperator() {
    override fun operate(base: MutableList<Any>, values: List<Any>): List<Any> {
        return values.fold(base, this::appendOnce)
    }

    private fun appendOnce(base: MutableList<Any>, value: Any): MutableList<Any> {
        return append(base, value, setOnce = true)
    }
}

internal object PropertyPrependOperator : ArrayPropertyOperator() {
    override fun operate(base: MutableList<Any>, values: List<Any>): List<Any> {
        return values.foldRight(base, this::prepend)
    }
}

internal object PropertyPrependOnceOperator : ArrayPropertyOperator() {
    override fun operate(base: MutableList<Any>, values: List<Any>): List<Any> {
        return values
            .fold(mutableListOf(), this::appendOnce)
            .foldRight(base, this::prependOnce)
    }

    private fun prependOnce(value: Any, base: MutableList<Any>): MutableList<Any> {
        return prepend(value, base, setOnce = true)
    }

    private fun appendOnce(base: MutableList<Any>, value: Any): MutableList<Any> {
        return append(base, value, setOnce = true)
    }
}

internal object PropertyRemoveOperator : ArrayPropertyOperator() {
    override fun operate(base: MutableList<Any>, values: List<Any>): List<Any> {
        return values.fold(base, this::remove)
    }

    private fun remove(builder: MutableList<Any>, value: Any): MutableList<Any> {
        builder.removeAll { PropertyOperators.equals(it, value) }
        return builder
    }
}

internal object PropertyClearAllOperator : PropertyOperator {
    override fun operate(base: Map<String, Any>, properties: Map<String, Any>): Map<String, Any> {
        return emptyMap()
    }
}
