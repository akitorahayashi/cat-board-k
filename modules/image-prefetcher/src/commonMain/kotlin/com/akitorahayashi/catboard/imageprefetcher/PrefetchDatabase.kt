package com.akitorahayashi.catboard.imageprefetcher

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [PrefetchedCatImageURL::class], version = 1)
@ConstructedBy(DatabaseConstructor::class)
abstract class PrefetchDatabase : RoomDatabase() {
    abstract fun queue(): Queue
}

@Suppress("KotlinNoActualForExpect")
expect object DatabaseConstructor : RoomDatabaseConstructor<PrefetchDatabase> {
    override fun initialize(): PrefetchDatabase
}
