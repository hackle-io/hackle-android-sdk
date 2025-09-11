package io.hackle.android.ui.core

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

internal class DrawablesTest {

    @Test
    fun `transparent should return transparent ColorDrawable`() {
        val drawable = Drawables.transparent()

        expectThat(drawable).isA<ColorDrawable>()
        expectThat(drawable.color).isEqualTo(Color.TRANSPARENT)
    }

    @Test
    fun `of should create GradientDrawable with default parameters`() {
        val color = Color.argb(255, 255, 0, 0) // Red color
        
        val drawable = Drawables.of(color = color)

        expectThat(drawable).isA<GradientDrawable>()
        expectThat(drawable.shape).isEqualTo(GradientDrawable.RECTANGLE)
    }
}
