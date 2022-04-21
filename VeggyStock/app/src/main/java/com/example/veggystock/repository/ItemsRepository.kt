package com.example.veggystock.repository

import com.example.veggystock.model.DAO
import com.example.veggystock.model.ItemsDataClass

class ItemsRepository(private val dao: DAO) {

    val items = dao.getAll()

    suspend fun insert(data: ItemsDataClass): Long {
        return dao.insert(data)
    }

    suspend fun update(data: ItemsDataClass): Int {
        return dao.update(data)
    }

    suspend fun delete(data: ItemsDataClass): Int {
        return dao.delete(data)
    }

    suspend fun drop(): Int {
        return dao.deleteAll()
    }
}