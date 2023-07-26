package io.hackle.android.ui.explorer.base

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

internal abstract class ListAdapter<ITEM, VIEW_HOLDER : ViewHolder<ITEM>> : BaseAdapter() {

    private var items = mutableListOf<ITEM>()

    abstract fun createViewHolder(parent: ViewGroup?): VIEW_HOLDER

    fun update(items: List<ITEM>) {
        this.items = ArrayList(items)
        refresh()
    }

    fun update(item: ITEM, position: Int) {
        items.add(position, item)
        refresh()
    }

    private fun refresh() {
        notifyDataSetChanged()
    }

    final override fun getCount(): Int {
        return items.size
    }

    final override fun getItem(position: Int): ITEM {
        return items[position]
    }

    final override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    final override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val holder = if (convertView == null) {
            createViewHolder(parent)
                .also { it.itemView.tag = it }
        } else {
            @Suppress("UNCHECKED_CAST")
            convertView.tag as VIEW_HOLDER
        }

        holder.bind(items[position])
        return holder.itemView
    }
}
