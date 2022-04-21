package com.example.veggystock.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.veggystock.repository.ItemsRepository

class ItemsViewModelFactory(
    private val repository: ItemsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemsViewModel::class.java)) {
            return ItemsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown View Model")
    }
}