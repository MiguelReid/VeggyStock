package com.example.veggystock

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.veggystock.databinding.ActivityAllItemsBinding
import com.example.veggystock.modelDB.Body
import com.example.veggystock.recycler.Adapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joery.animatedbottombar.AnimatedBottomBar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*


class Items : AppCompatActivity() {
    lateinit var binding: ActivityAllItemsBinding
    lateinit var adapter: Adapter
    val list: MutableList<Body> = ArrayList()
    val arrayList = ArrayList<Body>()
    private lateinit var reference: DatabaseReference
    private lateinit var db: FirebaseDatabase
    private val regex = Regex("[^A-Za-z0-9]")
    private lateinit var email: String
    private val scope = CoroutineScope(Dispatchers.IO)
    private val scope2 = CoroutineScope(Dispatchers.IO)
    private lateinit var data2: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAllItemsBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val intent = intent
        val emailAux = intent.getStringExtra("EMAIL")
        email = regex.replace(emailAux.toString(), "")
        initDB()
        recycler()
        search()
        swipe()
        menu()
        getData()
    }

    fun getData(): HashMap<String, String> {
        // For sending information to the adapter
        val map = HashMap<String, String>()
        map["EMAIL"] = email
        return map
    }

    fun bitmapToUri(imageBitmap: Bitmap, cacheDir: File): Uri {
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        data2 = baos.toByteArray()
        val file = File(cacheDir, imageBitmap.toString())
        // When calling it with imageBitmap.toString() each item has its image (before the same one repeated)
        file.delete()
        // Just in case there is another File
        file.createNewFile()
        val fileOS = FileOutputStream(file)
        fileOS.write(data2)
        fileOS.flush()
        fileOS.close()
        baos.close()
        return file.toUri()
    }

    private fun menu() {
        binding.topAppBar?.setNavigationOnClickListener {
            // Handle navigation icon press
        }

        binding.topAppBar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.orderBy -> {
                    orderByMenu()
                    true
                }
                R.id.item_new -> {
                    newItem()
                    true
                }
                else -> false
            }
        }

        binding.bottomBar?.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                when (newIndex) {
                    0 -> scope.launch { fillAll() }
                    1 -> scope.launch { fillFavourites() }
                }
            }
        })
    }

    private fun fillFavourites() {
        list.clear()

        reference =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("Users").child(email)

        reference.orderByChild("favourite").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (itemSnapshot in snapshot.children) {
                            val item = itemSnapshot.getValue(Body::class.java)
                            list.add(item!!)
                        }
                    }
                    adapter = Adapter(list)
                    binding.recycler.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun orderByMenu() {

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
                adapter.removeAt(viewHolder.absoluteAdapterPosition, email)
                Snackbar.make(binding.recycler, R.string.item_deleted, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
        val ith = ItemTouchHelper(touchHelper)
        ith.attachToRecyclerView(binding.recycler)
    }

    private fun search() {
        scope2.launch {
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
    }

    private fun recycler() {
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.setHasFixedSize(true)
        scope.launch { fillAll() }
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
        reference =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("Users").child(email)
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

    private fun newItem() {
        val i = Intent(this, NewItem::class.java).apply {
            putExtra("EMAIL", email)
        }
        startActivity(i)
    }
}