package io.hackle.android.ui.explorer.base

import android.view.View

internal interface ViewHolder<in ITEM> {

    val itemView: View

    fun bind(item: ITEM)
}
