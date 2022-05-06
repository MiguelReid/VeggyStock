package com.example.veggystock

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.veggystock.databinding.ActivityAllItemsBinding
import com.example.veggystock.modelDB.Body
import com.example.veggystock.recycler.Adapter
import com.google.firebase.database.*
import java.util.*


class Items : AppCompatActivity() {
    lateinit var binding: ActivityAllItemsBinding
    lateinit var adapter: Adapter
    val list: MutableList<Body> = ArrayList()
    val arrayList = ArrayList<Body>()
    private lateinit var reference: DatabaseReference
    private lateinit var db: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAllItemsBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initDB()
        recycler()
        search()
        swipe()
        menu()
    }

    private fun menu() {
        binding.topAppBar?.setNavigationOnClickListener {
            // Handle navigation icon press
        }

        binding.topAppBar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.favorite -> {
                    favorite()
                    true
                }
                R.id.more -> {
                    more()
                    true
                }
                R.id.item_new -> {
                    newItem()
                    true
                }
                else -> false
            }
        }
    }

    private fun favorite() {

    }

    private fun more() {

    }

    private fun swipe() {
        val touchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.recycler.adapter as Adapter
                adapter.removeAt(viewHolder.adapterPosition)
            }
        }
        val ith = ItemTouchHelper(touchHelper)
        ith.attachToRecyclerView(binding.recycler)
    }

    private fun search() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                if (p0 != null) {
                    if (p0.isNotEmpty()) {
                        arrayList.clear()
                        val search = p0.lowercase(Locale.getDefault())
                        list.forEach {
                            if (it.name.lowercase(Locale.getDefault()).contains(search)) {
                                arrayList.add(it)
                                adapter = Adapter(arrayList)
                                binding.recycler.adapter = adapter
                            }
                        }
                        binding.recycler.adapter?.notifyItemRangeChanged(0, list.size)
                        return true
                    } else {
                        arrayList.clear()
                        fillAll()
                        adapter = Adapter(list)
                        binding.recycler.adapter = adapter
                        binding.recycler.adapter?.notifyItemRangeChanged(0, list.size)

                    }
                }
                return true
            }
        })
    }

    private fun recycler() {
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.setHasFixedSize(true)
        fillAll()
        adapter = Adapter(list)
        binding.recycler.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clear() {
        list.clear()
        adapter = Adapter(list)
        binding.recycler.adapter = adapter
        arrayList.clear()
        binding.recycler.adapter?.notifyDataSetChanged()
    }

    private fun initDB() {
        db =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
        reference = db.getReference("items")
    }

    private fun fillAll() {
        val intent = intent
        val email = intent.getStringExtra("EMAIL")
        val regex = Regex("[^A-Za-z0-9]")
        //Firebase Realtime Database doesn't accept special characters
        reference =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("Users").child(regex.replace(email.toString(), ""))
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                clear()
                if (snapshot.exists()) {
                    for (itemSnapshot in snapshot.children) {
                        val item = itemSnapshot.getValue(Body::class.java)
                        list.add(item!!)
                    }
                    adapter = Adapter(list)
                    binding.recycler.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Items, "Error Filling Recycler", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /*
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_new -> {
                newItem()
            }
            R.id.item_media -> {
                startActivity(Intent(this, Media::class.java))
            }
            R.id.item_fragment -> {
                startActivity(Intent(this, FragmentManager::class.java))
            }
            R.id.item_room -> {
                startActivity(Intent(this, Room::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }
    */

    private fun newItem() {
        val intent = intent
        val email = intent.getStringExtra("EMAIL")
        val i = Intent(this, NewItem::class.java).apply {
            putExtra("EMAIL", email)
        }
        startActivity(i)
        Log.d(TAG, "IS ACTIVITY STARTING??")
    }
}