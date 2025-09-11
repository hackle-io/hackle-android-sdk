package io.hackle.android.ui.core

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import io.mockk.*
import org.junit.Before
import org.junit.Test

internal class ImageLoaderTest {

    private val context = mockk<Context>()
    private val imageView = mockk<ImageView>()
    private val requestManager = mockk<RequestManager>()

    @Before
    fun setUp() {
        mockkStatic(Glide::class)
        every { Glide.with(any<Context>()) } returns requestManager
        every { requestManager.load(any<String>()) } returns mockk {
            every { apply(any<RequestOptions>()) } returns this
            every { into(any<ImageView>()) } returns mockk()
        }
    }

    @Test
    fun `GlideImageLoader should load image using Glide`() {
        val imageLoader = GlideImageLoader()
        val url = "https://example.com/image.jpg"

        imageLoader.renderTo(context, url, imageView)

        verify { Glide.with(context) }
        verify { requestManager.load(url) }
    }

    @Test
    fun `GlideImageLoader should handle empty URL`() {
        val imageLoader = GlideImageLoader()
        val url = ""

        imageLoader.renderTo(context, url, imageView)

        verify { Glide.with(context) }
        verify { requestManager.load(url) }
    }

    @Test
    fun `GlideImageLoader should handle different contexts`() {
        val imageLoader = GlideImageLoader()
        val context1 = mockk<Context>()
        val context2 = mockk<Context>()
        val url = "https://example.com/image.jpg"
        val imageView1 = mockk<ImageView>()
        val imageView2 = mockk<ImageView>()

        imageLoader.renderTo(context1, url, imageView1)
        imageLoader.renderTo(context2, url, imageView2)

        verify { Glide.with(context1) }
        verify { Glide.with(context2) }
    }
}