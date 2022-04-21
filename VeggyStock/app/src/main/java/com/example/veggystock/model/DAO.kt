package com.example.veggystock.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DAO {

    @Insert
    suspend fun insert(itemsDataClass: ItemsDataClass): Long

    @Update
    suspend fun update(itemsDataClass: ItemsDataClass): Int

    @Delete
    suspend fun delete(itemsDataClass: ItemsDataClass): Int

    @Query("DELETE FROM items")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM items")
    fun getAll(): Flow<List<ItemsDataClass>>
}