package com.example.veggystock

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.veggystock.databinding.ActivityNewItemBinding
import com.example.veggystock.modelDB.Body
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class NewItem : AppCompatActivity() {

    private var cameraCode: Int = 666
    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var storage: StorageReference
    lateinit var binding: ActivityNewItemBinding
    private lateinit var imageUri: Uri
    private lateinit var imageBitmap: Bitmap
    private lateinit var data2: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        binding = ActivityNewItemBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        listener()
        binding.imageButton.setImageResource(R.drawable.nophoto)
    }

    private fun listener() {
        binding.btnSave.setOnClickListener {
            saveItem()
            onBackPressed()
        }
        binding.imageButton.setOnClickListener {
            if (binding.switchCamera!!.isChecked) requestPermission()
            else chooseImage()
        }
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "You need the camera!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraCode
            )
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager)?.also {
                        startActivityForResult(takePictureIntent, cameraCode)
                    }
                }
            } else {
                Toast.makeText(this, "Let me see :(", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            intent,
            100
        )
    }

    private fun saveItem() {
        val name = binding.inputName?.editText?.text.toString()
        val provider = binding.inputProvider?.editText?.text.toString()
        val aux = binding.inputPrice?.editText?.text.toString()
        val price = aux.toFloat()
        val rating = binding.ratingBar.rating
        val address = binding.inputStreet?.editText?.text.toString()
        val fileName = "$name $provider $address"
        initDB()
        uploadImage()
        reference.child(fileName)
            .setValue(Body(name, provider, price, address, rating)).addOnSuccessListener {
            }.addOnFailureListener {
                Toast.makeText(this, "Item Not Saved", Toast.LENGTH_SHORT).show()
            }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            imageUri = data?.data!!
            Picasso.get()!!.load(imageUri).resize(300, 300).centerCrop().into(binding.imageButton)
        }

        if (requestCode == cameraCode && resultCode == RESULT_OK) {
            imageBitmap = data?.extras?.get("data") as Bitmap

            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            data2 = baos.toByteArray()

            val file = File(cacheDir, "filename.jpg")
            file.createNewFile()
            val fileOS = FileOutputStream(file)
            fileOS.write(data2)
            fileOS.flush()
            fileOS.close()
            binding.imageButton.setImageBitmap(imageBitmap)
        }
    }

    private fun uploadImage() {
        val name = binding.inputName?.editText?.text.toString()
        val provider = binding.inputProvider?.editText?.text.toString()
        val address = binding.inputStreet?.editText?.text.toString()
        val fileName = "$name $provider $address"

        storage = FirebaseStorage.getInstance().getReference("images/$fileName")

        if (binding.switchCamera?.isChecked!!) {
            storage.putBytes(data2).addOnSuccessListener {
                //We have to putBytes because its in ByteArray format, and not Uri like normally
                binding.imageButton.setImageURI(null)
            }.addOnFailureListener {
                Toast.makeText(this@NewItem, "Image Not Uploaded", Toast.LENGTH_SHORT).show()
            }
        } else {
            storage.putFile(imageUri).addOnSuccessListener {
                binding.imageButton.setImageURI(null)
            }.addOnFailureListener {
                Toast.makeText(this@NewItem, "Image Not Uploaded", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun initDB() {
        db =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
        reference = db.getReference("items")
    }
}