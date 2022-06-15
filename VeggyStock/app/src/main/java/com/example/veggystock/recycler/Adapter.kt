package com.example.veggystock.recycler

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.veggystock.Items.Items
import com.example.veggystock.R
import com.example.veggystock.databinding.ActivityItemBinding
import com.example.veggystock.modelDB.Body
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File


private lateinit var reference: DatabaseReference
private lateinit var db: FirebaseDatabase
var favourite = false
val scope = CoroutineScope(Dispatchers.IO)
private lateinit var uri: Uri

/**
 * La clase adapter es dede donde controlare cada
 * funcion especifica a un item dependiendo siempre
 * de la posicion
 * @property list
 */

class Adapter(private val list: MutableList<Body>) : RecyclerView.Adapter<Adapter.DataHolder>() {

    /**
     * Cogemos el binding
     * @constructor
     * @param v
     */

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

    /**
     * Desde aqui cogemos toda la informacion de cada item y su imagen
     * Abrimos el intent de google maps para la ubicacion
     * Manejamos el valor favorito con realtime database y
     * vemos si es vegano o no para hacer visible el pequeño icon
     * @param holder
     * @param position
     */

    override fun onBindViewHolder(holder: DataHolder, position: Int) {
        initDB()
        val activity: Items = holder.itemView.context as Items
        val map = activity.getData()
        val email = map["EMAIL"]
        // Con este HashMap podemos coger informacion desde otro activity

        /*
        val newitem = NewItem()
        val map2 = newitem.sendData()
        val fileName = map2["FILENAME"]
        Log.i("INFO FILENAME ADAPTER ->>", fileName.toString())
         */

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
        val localfile = File.createTempFile("tempImage", "jpg")
        // File.createTempFile se pone fuera ya que no puede estar dentro
        // de un corrutine scope

        scope.launch {
            /*
            if (fileName.equals(imageName)) {
                delay(1500)
                Log.d("INFO NEW ITEM ->>", holder.binding.tvName.text.toString())
            }
             */
            val defered = async {
                storageRef.getFile(localfile).addOnSuccessListener {
                    val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                    //holder.binding.btnItem.setImageBitmap(bitmap)
                    val items = Items()
                    uri = items.bitmapToUri(bitmap, context.cacheDir)
                    Picasso.get().load(uri).fit().into(holder.binding.btnItem)
                }.addOnFailureListener {
                    Log.e("ERROR ->> ", "Failed to retrieve the image")
                }
            }
            defered.await()
            // Usando async y await nos aseguramos de que se espere a que ejecute
            // El subir la imagen
        }

        holder.binding.btnItem.setOnClickListener {
            val searchAddress =
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${element.address}"))
            context.startActivity(searchAddress)
        }
        // Abrimos el intent de google maps con la direccion establecida

        holder.binding.imgHeart?.setOnClickListener {
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
                Snackbar.make(holder.itemView, R.string.marked_favourite, Snackbar.LENGTH_SHORT)
                    .show()
                true
            } else {
                holder.binding.imgHeart.setImageResource(R.drawable.heartfalse)
                reference.setValue(false)
                Snackbar.make(holder.itemView, R.string.marked_not_favourite, Snackbar.LENGTH_SHORT)
                    .show()
                false
            }
        }
        reference =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("Users").child(email.toString()).child(imageName).child("vegan")

        reference.get().addOnSuccessListener {
            if (it.value == true) {
                holder.binding.imageVeggy?.visibility = View.VISIBLE
            } else {
                holder.binding.imageVeggy?.visibility = View.INVISIBLE
            }
        }.addOnFailureListener {
            Log.e("ERROR ->> ", "Vegan Icon error getting data", it)
        }
    }

    /**
     * Count de los elementos en la lista
     * @return
     */

    override fun getItemCount(): Int {
        return list.count()
    }

    /**
     * Eliminamos el child de realtime database y
     * la imagen con el mismo nombre
     * @param position
     * @param email
     */

    fun removeAt(position: Int, email: String) {
        val element = list[position]
        list.removeAt(position)

        val name = element.name
        val provider = element.provider
        val address = element.address
        val imageName = "$name $provider $address"

        val ref = FirebaseDatabase.getInstance().reference
        val query = ref.child("Users").child(email).orderByChild("name").equalTo(name)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (appleSnapshot in dataSnapshot.children) {
                    appleSnapshot.ref.removeValue()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ERROR ->>", "onCancelled", databaseError.toException())
            }
        })
        deleteImage(imageName)
        notifyItemRemoved(position)
    }

    /**
     * Se elimina la imagen de storage
     * @param imageName
     */

    private fun deleteImage(imageName: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("images/$imageName")
        storageRef.delete()
            .addOnSuccessListener {
                Log.i("Success ->> ", "Image Deleted")
            }.addOnFailureListener {
                Log.e("ERROR ->> ", " image not deleted from Storage")
            }
    }

    /**
     * Inicializamos la base de datos
     */

    private fun initDB() {
        db =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
    }
}