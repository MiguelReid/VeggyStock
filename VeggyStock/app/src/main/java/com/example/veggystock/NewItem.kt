package com.example.veggystock

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.veggystock.databinding.ActivityNewItemBinding
import com.example.veggystock.modelDB.Body
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.squareup.picasso.Picasso
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    private var urlBase = "https://api.edamam.com/api/food-database/v2/parser?session=40&"

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        binding = ActivityNewItemBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        listener()
        menu()
        //binding.imageButton.setImageResource(R.drawable.nophoto)
        check()
    }
    //private fun translate(sentence: String): String {}

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(urlBase)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun check() {
        binding.btnSave.isEnabled = false
        val editTexts = listOf(
            binding.inputName?.editText,
            binding.inputProvider?.editText,
            binding.inputPrice?.editText,
            binding.inputStreet?.editText
        )
        for (editText in editTexts) {
            editText?.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val et1 = binding.inputName?.editText?.text.toString().trim()
                    val et2 = binding.inputProvider?.editText?.text.toString().trim()
                    val et3 = binding.inputPrice?.editText?.text.toString().trim()
                    val et4 = binding.inputStreet?.editText?.text.toString().trim()

                    binding.btnSave.isEnabled = et1.isNotEmpty()
                            && et2.isNotEmpty()
                            && et3.isNotEmpty()
                            && et4.isNotEmpty()
                }

                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun afterTextChanged(
                    s: Editable
                ) {
                }
            })
        }
    }

    private fun menu() {
        binding.topBarNewItem?.setNavigationOnClickListener {
            // Handle navigation icon press
        }
        /*
        binding.topBarNewItem?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.scan -> {
                    requestPermission()
                    true
                }
                else -> false
            }
        }
         */
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
            .setValue(Body(name, provider, price, address, rating, false)).addOnSuccessListener {
            }.addOnFailureListener {
                Toast.makeText(this, "Item Not Saved", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scanBarcodes(bitmap: Bitmap) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()

        val scanner = BarcodeScanning.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                // This does work
                for (barcode in barcodes) {
                    val bounds = barcode.boundingBox
                    val corners = barcode.cornerPoints
                    val rawValue = barcode.rawValue
                    val valueType = barcode.valueType
                    Log.d("RAWVALUE ->>>", rawValue.toString())

                    when (valueType) {
                        Barcode.FORMAT_UPC_A -> {
                            Log.d("TASK SUCCESFUL ->>>>>>", "UPC A")
                            val upc = barcode.url
                        }
                        Barcode.FORMAT_UPC_E -> {
                            Log.d("TASK SUCCESFUL ->>>>>>", "UPC E")
                        }
                        Barcode.FORMAT_CODABAR -> {
                            Log.d("TASK SUCCESFUL ->>>>>>", "BARCODE")
                            val title = barcode.url!!.title
                            val url = barcode.url!!.url
                        }
                        Barcode.FORMAT_QR_CODE -> {
                            Log.d("TASK SUCCESFUL ->>", "QR CODE")
                        }
                        Barcode.TYPE_TEXT -> {
                            val data = barcode.displayValue
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.d("PROBLEM ->>>>>>", "OOPSIE")
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            imageUri = data?.data!!
            if (binding.switchCamera?.isChecked == true)
                Picasso.get()!!.load(imageUri).resize(300, 300).centerCrop()
                    .into(binding.imageButton)
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
            if (binding.switchCamera?.isChecked == true) {
                binding.imageButton.setImageBitmap(imageBitmap)
            } else {
                scanBarcodes(imageBitmap)
            }
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
        val intent = intent
        val email = intent.getStringExtra("EMAIL")
        db =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
        reference = db.getReference("Users").child(email.toString())
    }
}