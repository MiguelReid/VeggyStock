package com.example.veggystock.recycler

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.veggystock.Items
import com.example.veggystock.R
import com.example.veggystock.databinding.ActivityItemBinding
import com.example.veggystock.modelDB.Body
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File

private lateinit var reference: DatabaseReference
private lateinit var db: FirebaseDatabase
var favourite = false

class Adapter(private val list: MutableList<Body>) : RecyclerView.Adapter<Adapter.DataHolder>() {

    class DataHolder(v: View) : RecyclerView.ViewHolder(v) {
        val binding = ActivityItemBinding.bind(v)
    }

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val v = layoutInflater.inflate(R.layout.activity_item, parent, false)
        return DataHolder(v)
    }

    override fun onBindViewHolder(holder: DataHolder, position: Int) {
        initDB()
        val element = list[position]
        holder.binding.tvName.text = element.name
        holder.binding.tvProvider.text = element.provider
        holder.binding.tvPrice.text = element.price.toString()
        holder.binding.tvAddress.text = element.address
        holder.binding.ratingBar.rating = element.rating

        val name = element.name
        val provider = element.provider
        val address = element.address

        val imageName = "$name $provider $address"

        val storageRef = FirebaseStorage.getInstance().reference.child("images/$imageName")
        val localfile =
            File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {

            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.binding.btnItem.setImageBitmap(bitmap)
            //Picasso.get()!!.load(bitmap).resize(138, 166).centerCrop().into(holder.binding.btnItem)
        }.addOnFailureListener {
            Log.d(TAG, "Failed to retrieve the image")
        }

        holder.binding.btnItem.setOnClickListener {
            val searchAddress =
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${element.address}"))
            context.startActivity(searchAddress)
        }
        holder.binding.btnItem.setOnLongClickListener {

            return@setOnLongClickListener true
        }

        holder.binding.imgHeart?.setOnClickListener {
            val activity: Items = holder.itemView.context as Items
            val map = activity.getData()
            val email = map["EMAIL"]

            reference =
                FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("Users").child(email.toString()).child(imageName)
                    .child("favourite")

            favourite = if (!favourite) {
                val animation = R.raw.heartlottie
                holder.binding.imgHeart.setAnimation(animation)
                holder.binding.imgHeart.playAnimation()
                holder.binding.imgHeart.setImageResource(R.drawable.hearttrue)
                reference.setValue(true)
                true
            } else {
                holder.binding.imgHeart.setImageResource(R.drawable.heartfalse)
                reference.setValue(false)
                false
            }
        }
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    fun removeAt(position: Int) {
        val element = list[position]
        list.removeAt(position)
        val ref = FirebaseDatabase.getInstance().reference
        val applesQuery = ref.child("items").orderByChild("name").equalTo(element.name)
        applesQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (appleSnapshot in dataSnapshot.children) {
                    appleSnapshot.ref.removeValue()
                    Log.d(TAG, appleSnapshot.toString())
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException())
            }
        })
        notifyItemRemoved(position)
    }

    private fun initDB() {
        db =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
        reference = db.getReference("items")
    }
}