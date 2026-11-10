package com.akitorahayashi.catboard.imagestore

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class StoredURLs {
    @Insert
    abstract suspend fun insert(images: List<StoredCatImageURL>)

    @Query("SELECT COUNT(*) FROM StoredCatImageURL")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM StoredCatImageURL ORDER BY id LIMIT :count")
    protected abstract suspend fun oldest(count: Int): List<StoredCatImageURL>

    @Query("DELETE FROM StoredCatImageURL WHERE id IN (:ids)")
    protected abstract suspend fun remove(ids: List<Long>)

    @Transaction
    open suspend fun take(count: Int): List<StoredCatImageURL> {
        require(count >= 0) { "count must not be negative" }
        val images = oldest(count)
        remove(images.map { it.id })
        return images
    }
}
