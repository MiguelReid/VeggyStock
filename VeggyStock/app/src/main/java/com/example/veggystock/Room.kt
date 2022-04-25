package com.example.veggystock

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.veggystock.databinding.ActivityRoomBinding
import com.example.veggystock.model.ItemsDatabase
import com.example.veggystock.repository.ItemsRepository
import com.example.veggystock.viewModel.ItemsViewModel
import com.example.veggystock.viewModel.ItemsViewModelFactory

class Room : AppCompatActivity() {
    private lateinit var binding: ActivityRoomBinding
    private lateinit var viewModel: ItemsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dao = ItemsDatabase.getInstance(application).itemsDAO
        val repository = ItemsRepository(dao)
        val factory = ItemsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ItemsViewModel::class.java]
        title = "Room Simple Database"
        observe()
        listener()
    }

    private fun listener() {
        binding.btnSaveRoom.setOnClickListener {
            saveItem()
        }
        binding.btnDrop.setOnClickListener {
            drop()
        }
    }

    private fun drop() {
        viewModel.drop()
        Log.d("Database Dropped", "")
    }

    private fun saveItem() {
        val name = binding.etNameRoom.text.toString()
        val provider = binding.etProviderRoom.text.toString()
        viewModel.save(name, provider)
        observe()
        clean()
    }

    private fun clean() {
        binding.etNameRoom.setText("")
        binding.etProviderRoom.setText("")
    }

    private fun observe() {
        viewModel.getSavedItems().observe(this, Observer {
            binding.tvItems.text = it.toString()
        })
    }
}