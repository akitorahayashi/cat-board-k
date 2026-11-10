package com.akitorahayashi.catboard.imagestore

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [StoredCatImageURL::class], version = 1)
@ConstructedBy(DatabaseConstructor::class)
abstract class UrlDatabase : RoomDatabase() {
    abstract fun urls(): StoredURLs
}

@Suppress("KotlinNoActualForExpect")
expect object DatabaseConstructor : RoomDatabaseConstructor<UrlDatabase> {
    override fun initialize(): UrlDatabase
}
