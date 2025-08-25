package io.hackle.android.mock

import android.os.Bundle
import io.mockk.every
import io.mockk.mockk

object MockBundle {
    fun create(map: Map<String, Any?>): Bundle {
        val bundle = mockk<Bundle>()
        for ((key, value) in map) {
            when (value) {
                is Boolean -> {
                    every { bundle.getBoolean(key) } returns value
                    every { bundle.getBoolean(key, any()) } returns value
                }
                is String -> {
                    every { bundle.getString(key) } returns value
                    every { bundle.getString(key, any()) } returns value
                }
                is Number -> {
                    every { bundle.getByte(key) } returns value.toByte()
                    every { bundle.getByte(key, any()) } returns value.toByte()

                    every { bundle.getChar(key) } returns value.toChar()
                    every { bundle.getChar(key, any()) } returns value.toChar()

                    every { bundle.getShort(key) } returns value.toShort()
                    every { bundle.getShort(key, any()) } returns value.toShort()

                    every { bundle.getInt(key) } returns value.toInt()
                    every { bundle.getInt(key, any()) } returns value.toInt()

                    every { bundle.getLong(key) } returns value.toLong()
                    every { bundle.getLong(key, any()) } returns value.toLong()

                    every { bundle.getFloat(key) } returns value.toFloat()
                    every { bundle.getFloat(key, any()) } returns value.toFloat()

                    every { bundle.getDouble(key) } returns value.toDouble()
                    every { bundle.getDouble(key, any()) } returns value.toDouble()
                }
                else -> throw UnsupportedOperationException("Type is not supported.")
            }
        }
        return bundle
    }
}
