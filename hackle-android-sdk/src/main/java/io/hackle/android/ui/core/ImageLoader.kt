package io.hackle.android.ui.core

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

internal interface ImageLoader {
    fun renderTo(context: Context, url: String, imageView: ImageView)
}

internal class GlideImageLoader : ImageLoader {

    override fun renderTo(context: Context, url: String, imageView: ImageView) {
        Glide.with(context)
            .load(url)
            .apply(requestOptions())
            .into(imageView)
    }

    private fun requestOptions(): RequestOptions {
        return RequestOptions().onlyRetrieveFromCache(false)
    }
}
