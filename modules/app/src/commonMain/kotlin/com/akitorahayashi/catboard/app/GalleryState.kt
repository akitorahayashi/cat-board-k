@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.akitorahayashi.catboard.app

@kotlin.native.HiddenFromObjC
data class GalleryImage(val id: Long, val imageURL: String)

@kotlin.native.HiddenFromObjC
data class GalleryState(
    val images: List<GalleryImage> = emptyList(),
    val isInitializing: Boolean = true,
    val isAdditionalFetching: Boolean = false,
    val errorMessage: String? = null,
    val warning: String? = null,
)
