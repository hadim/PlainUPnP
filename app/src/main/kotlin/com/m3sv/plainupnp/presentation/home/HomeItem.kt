package com.m3sv.plainupnp.presentation.home

import androidx.annotation.DrawableRes

data class ContentItem(
        val uri: String?,
        val thumbnailUri: String?,
        val name: String,
        val type: ContentType,
        @DrawableRes val icon: Int
)

enum class ContentType {
    IMAGE, AUDIO, VIDEO, DIRECTORY
}