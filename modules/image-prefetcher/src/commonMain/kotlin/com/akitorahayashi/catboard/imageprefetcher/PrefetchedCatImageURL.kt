package com.akitorahayashi.catboard.imageprefetcher

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PrefetchedCatImageURL(
    val imageURL: String,
    val screeningEnabled: Boolean,
    val scaryMode: Boolean,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
