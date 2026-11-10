package com.akitorahayashi.catboard.imagestore

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class StoredCatImageURL(
    val imageURL: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
