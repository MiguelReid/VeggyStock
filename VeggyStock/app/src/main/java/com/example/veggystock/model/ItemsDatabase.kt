package com.example.veggystock.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ItemsDataClass::class], version = 1, exportSchema = false)
abstract class ItemsDatabase : RoomDatabase() {
    abstract val itemsDAO: DAO

    companion object {
        @Volatile
        private var INSTANCE: ItemsDatabase? = null
        fun getInstance(context: Context): ItemsDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                    context.applicationContext,
                        ItemsDatabase::class.java,
                        "items"
                    ).build()
                }
                return instance
            }
        }
    }
}


