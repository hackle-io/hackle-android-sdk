package io.hackle.android.internal.properties

import io.hackle.sdk.common.PropertyOperation
import io.hackle.sdk.common.PropertyOperation.*
import io.hackle.sdk.common.PropertyOperations

internal object PropertyOperators {

    fun equals(a: Any, b: Any): Boolean {
        if (a is Number && b is Number) {
            return a.toDouble() == b.toDouble()
        }

        return a == b
    }

    operator fun get(operation: PropertyOperation): PropertyOperator {
        return when (operation) {
            SET -> PropertySetOperator
            SET_ONCE -> PropertySetOnceOperator
            APPEND -> PropertyAppendOperator
            APPEND_ONCE -> PropertyAppendOnceOperator
            PREPEND -> PropertyPrependOperator
            PREPEND_ONCE -> PropertyPrependOnceOperator
            REMOVE -> PropertyRemoveOperator
            INCREMENT -> PropertyIncrementOperator
            UNSET -> PropertyUnsetOperator
            CLEAR_ALL -> PropertyClearAllOperator
        }
    }
}

internal val PropertyOperation.operator get() = PropertyOperators[this]

internal fun PropertyOperation.operate(
    base: Map<String, Any>,
    properties: Map<String, Any>,
): Map<String, Any> {
    return operator.operate(base, properties)
}

internal fun PropertyOperations.operate(base: Map<String, Any>): Map<String, Any> {
    var accumulator = base
    for ((operation, properties) in this.asMap()) {
        accumulator = operation.operate(accumulator, properties)
    }
    return accumulator
}
