package com.example.veggystock.viewModel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.veggystock.model.ItemsDataClass
import com.example.veggystock.repository.ItemsRepository
import com.example.veggystock.view.Event
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ItemsViewModel(private val repository: ItemsRepository) : ViewModel() {
    private lateinit var items: ItemsDataClass

    fun save(name: String, provider:String) {
        insertItem(ItemsDataClass(0, name, provider))
    }

    private fun insertItem(data: ItemsDataClass) = viewModelScope.launch {
        val newRow = repository.insert(data)
        if (newRow > -1) {
            Log.d("ITEM ADDED!","ITEM ADDED->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        } else {
            Log.d("ERROR WHILE INSERTING!","ERROR->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        }
    }

    fun getSavedItems() = liveData {
        repository.items.collect {
            emit(it)
        }
    }

    fun deleteItem(items: ItemsDataClass) = viewModelScope.launch {
        val numberRowsDeleted = repository.delete(items)
        if (numberRowsDeleted > 0) {
        } else {
        }
    }

    fun drop() = viewModelScope.launch {
        val numberRowsDeleted = repository.drop()
        if (numberRowsDeleted > 0) {
            Log.d("DATABASE DROPPED!","DATABASE DROPPED->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        } else {
            Log.d("DATABASE NOT DROPPED!","DATABASE NOT DROPPED->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        }
    }
}