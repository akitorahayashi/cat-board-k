package com.akitorahayashi.catboard.imageprefetcher

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class Queue {
    @Insert
    abstract suspend fun insert(images: List<PrefetchedCatImageURL>)

    @Query("SELECT COUNT(*) FROM PrefetchedCatImageURL WHERE screeningEnabled = :enabled AND scaryMode = :scary")
    abstract suspend fun count(enabled: Boolean, scary: Boolean): Int

    @Query("SELECT * FROM PrefetchedCatImageURL WHERE screeningEnabled = :enabled AND scaryMode = :scary ORDER BY id LIMIT :count")
    protected abstract suspend fun oldest(count: Int, enabled: Boolean, scary: Boolean): List<PrefetchedCatImageURL>

    @Query("DELETE FROM PrefetchedCatImageURL WHERE id IN (:ids)")
    protected abstract suspend fun remove(ids: List<Long>)

    @Query("DELETE FROM PrefetchedCatImageURL")
    abstract suspend fun clear()

    @Transaction
    open suspend fun take(count: Int, enabled: Boolean, scary: Boolean): List<String> {
        require(count >= 0) { "count must not be negative" }
        val images = oldest(count, enabled, scary)
        remove(images.map { it.id })
        return images.map { it.imageURL }
    }
}
