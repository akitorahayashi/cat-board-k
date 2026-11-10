package com.akitorahayashi.catboard.app

data class GalleryImage(val id: Long, val imageURL: String)

data class GalleryState(
    val images: List<GalleryImage> = emptyList(),
    val isInitializing: Boolean = true,
    val isAdditionalFetching: Boolean = false,
    val errorMessage: String? = null,
    val warning: String? = null,
)
