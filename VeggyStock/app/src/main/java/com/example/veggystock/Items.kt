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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private val healthTags = arrayOf("Vegan", "Vegetarian", "Gluten-Free")
    private val orderTags = arrayOf("Name", "Price", "Rating")

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAllItemsBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val intent = intent
        val emailAux = intent.getStringExtra("EMAIL")
        email = regex.replace(emailAux.toString(), "")
        initDB()
        scope.launch { recycler() }
        search()
        swipe()
        menu()
        scope2.launch { getData() }
    }

    /**
     * Hash map I can use to send information to the Adapter class
     * this way I can use the email to modify data in realtime database
     * @return
     */

    fun getData(): HashMap<String, String> {
        val map = HashMap<String, String>()
        map["EMAIL"] = email
        return map
    }

    /**
     * Function to convert a Bitmap into Uri
     * to manipulate them with Picasso
     * @param imageBitmap
     * @param cacheDir
     * @return
     */

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

    /**
     * What to do depending on
     * the element of the menu pressed
     */

    private fun menu() {
        binding.topAppBar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.orderBy -> {
                    alertDialog(orderTags)
                    true
                }
                R.id.item_new -> {
                    newItem()
                    true
                }
                R.id.maps -> {
                    alertDialog(healthTags)
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
                    0 -> scope.launch { fillAll("name") }
                    1 -> scope.launch { fillFavourites() }
                }
            }
        })
    }

    /**
     * Alert dialog giving options of
     * different restaurants you can eat in
     */

    private fun alertDialog(tags: Array<String>) {
        var checkedItem = 0

        MaterialAlertDialogBuilder(this, R.style.alertDialogInconclusive)
            .setTitle(resources.getString(R.string.select_option))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                if (tags.first() == "Vegan") {
                    googleMaps(tags[checkedItem])
                } else if (tags.first() == "Name") {
                    fillAll(tags[checkedItem])
                }
            }
            .setSingleChoiceItems(tags, checkedItem) { _, which ->
                checkedItem = which
            }
            .show()
    }

    /**
     * It starts an intent with the
     * selection made in the alert dialog
     * @param checked
     */

    private fun googleMaps(checked: String) {
        val searchAddress =
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=bar $checked&t=k&hl=es-419"))
        searchAddress.setPackage("com.google.android.apps.maps")
        startActivity(searchAddress)
    }

    /**
     * Filling the recyclerView with only items that have
     * the favourite value in realtime database = true
     */

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

    /**
     * It makes it possible to delete an item
     * when swiping it to the left
     */

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

    /**
     * So it filters the recyclerView with whatever
     * you type into the searchView, it compares it
     * to the name
     */

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
                            fillAll("name")
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

    /**
     * Initializing the recycler and setting
     * the adapter to the recycler's
     */

    private fun recycler() {
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.setHasFixedSize(true)
        scope.launch { fillAll("name") }
        adapter = Adapter(list)
        binding.recycler.adapter = adapter
    }

    /**
     * It makes it able to clear the main list and the search one
     * just in case it was getting repeated values
     */

    @SuppressLint("NotifyDataSetChanged")
    private fun clear() {
        list.clear()
        adapter = Adapter(list)
        binding.recycler.adapter = adapter
        arrayList.clear()
        binding.recycler.adapter?.notifyDataSetChanged()
    }

    /**
     * It initializes realtime database
     */

    private fun initDB() {
        db =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
        reference = db.getReference("items")
    }

    /**
     * It fills the recyclerView with all
     * of the items in the database
     */

    private fun fillAll(order:String) {
        reference =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("Users").child(email)
        reference.orderByChild(order.lowercase()).addValueEventListener(object : ValueEventListener {
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

    /**
     * It send you to the newItem activity
     * via an intent
     */

    private fun newItem() {
        val i = Intent(this, NewItem::class.java).apply {
            putExtra("EMAIL", email)
        }
        startActivity(i)
    }
}