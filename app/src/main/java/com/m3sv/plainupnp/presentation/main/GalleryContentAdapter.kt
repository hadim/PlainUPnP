package com.m3sv.plainupnp.presentation.main

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.m3sv.plainupnp.R
import com.m3sv.plainupnp.common.ItemsDiffCallback
import com.m3sv.plainupnp.data.upnp.DIDLItem
import com.m3sv.plainupnp.databinding.GalleryContentFolderItemBinding
import com.m3sv.plainupnp.databinding.MobileItemGalleryContentBinding
import com.m3sv.plainupnp.presentation.base.BaseAdapter
import com.m3sv.plainupnp.presentation.base.ItemViewHolder
import com.m3sv.plainupnp.presentation.main.data.ContentType
import com.m3sv.plainupnp.presentation.main.data.Item


interface OnItemClickListener {
    fun onDirectoryClick(directoryName: String, itemUri: String?, parentId: String?)

    fun onItemClick(item: DIDLItem, position: Int)
}

class GalleryContentAdapter(private val glide: RequestManager,
                            private val onItemClickListener: OnItemClickListener,
                            private val sharedPreferences: SharedPreferences) : BaseAdapter<Item>(diffCallback) {

    private val emptyRequestOptions = RequestOptions()

    override fun getItemViewType(position: Int): Int = items[position].type.ordinal

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): ItemViewHolder<ViewDataBinding> = when (ContentType.values()[viewType]) {
        ContentType.DIRECTORY -> ItemViewHolder(
                GalleryContentFolderItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                ), onItemClickListener
        )

        else -> ItemViewHolder(
                MobileItemGalleryContentBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                ), onItemClickListener
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder<ViewDataBinding>, position: Int) {
        val item = items[position]

        holder.bind(item)

        when (item.type) {
            ContentType.DIRECTORY -> loadDirectory(holder, item)
            else -> loadData(holder, item)
        }
    }

    private inline fun <T : ViewDataBinding> ItemViewHolder<*>.extractBinding(): T = (this as ItemViewHolder<T>).binding

    private fun loadData(
            holder: ItemViewHolder<ViewDataBinding>,
            item: Item,
            requestOptions: RequestOptions = emptyRequestOptions
    ) {

        with(holder.extractBinding<MobileItemGalleryContentBinding>()) {
            if (sharedPreferences.getBoolean("pref_enable_thumbnails", true))
                when (item.type) {
                    ContentType.IMAGE, ContentType.VIDEO -> glide.load(item.uri)
                            .thumbnail(0.1f)
                            .apply(requestOptions)
                            .into(thumbnail)
                    else -> thumbnail.setImageResource(item.icon)
                }
            else
                thumbnail.setImageResource(item.icon)

            title.text = item.name
            contentType.setImageResource(item.icon)
        }
    }

    private fun loadDirectory(
            holder: ItemViewHolder<*>,
            item: Item
    ) {
        with(holder.extractBinding<GalleryContentFolderItemBinding>()) {
            title.text = item.name
            thumbnail.setImageResource(R.drawable.ic_folder)
            container.setOnClickListener {
                onItemClickListener.onDirectoryClick(item.name, item.uri, item.parentId)
            }
        }
    }

    fun filter(text: CharSequence) {
        if (text.isEmpty()) {
            resetItems()
            return
        }

        filterWithDiff { it.name.toLowerCase().contains(text) }
    }


    class DiffCallback(
            oldItems: List<Item>,
            newItems: List<Item>
    ) : ItemsDiffCallback<Item>(oldItems, newItems) {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldItems[oldItemPosition].uri == newItems[newItemPosition].uri
    }

    companion object {
        val diffCallback = DiffCallback(listOf(), listOf())
    }
}