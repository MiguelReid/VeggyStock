package com.example.veggystock

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
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
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initDB()
        recycler()
        search()
        swipe()
        //menu()
        topBar()
    }

    private fun topBar() {
        topAppBar.setNavigationOnClickListener {
            // Handle navigation icon press
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.favorite -> {
                    // Handle favorite icon press
                    true
                }
                R.id.search -> {
                    // Handle search icon press
                    true
                }
                R.id.more -> {
                    // Handle more item (inside overflow menu) press
                    true
                }
                else -> false
            }
        }
    }

    private fun menu() {
        val callback = object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menuInflater.inflate(R.menu.contextual_action_bar, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.share -> {
                        share()
                        true
                    }
                    R.id.delete -> {
                        delete()
                        true
                    }
                    R.id.more -> {
                        more()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
            }
        }
        val actionMode = startSupportActionMode(callback)
        actionMode?.title = "1 selected"
    }

    private fun delete() {

    }

    private fun more() {

    }

    private fun share() {

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
        reference =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("items")
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

    private fun newItem() {
        val i = Intent(this, NewItem::class.java)
        startActivity(i)
    }
}