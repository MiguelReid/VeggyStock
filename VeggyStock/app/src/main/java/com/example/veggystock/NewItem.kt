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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.veggystock.databinding.ActivityNewItemBinding
import com.example.veggystock.foodDatabase.ApiService
import com.example.veggystock.foodDatabase.Gson2
import com.example.veggystock.modelDB.Body
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
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
    private var urlBaseDatabase = "https://api.edamam.com/api/food-database/v2/"
    private var appIdDatabase = "f92aec81"
    private var appKeyDatabase = "0efe25b2bec1d420f5f78f7deaa3358c"
    private var urlBaseNutrition = "https://api.edamam.com/api/"
    private var appIdNutrition = "d1e4b94c"
    private var appKeyNutrition = "f461b422fb6f8acec69fe9b7badc15d8"
    lateinit var apiCall2: Response<Gson2>
    lateinit var apiCall2Body: Gson2
    private var alert: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        binding = ActivityNewItemBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        listener()
        menu()
        binding.imageButton.setImageResource(R.drawable.nophoto)
        check()
    }

    private fun getRetrofit(urlBase: String): Retrofit {
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
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun afterTextChanged(s: Editable) {}
            })
        }
    }

    private fun menu() {
        binding.topBarNewItem?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.scan_item -> {
                    requestPermission()
                    true
                }
                else -> false
            }
        }
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
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13
            ).build()

        val scanner = BarcodeScanning.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue.toString()
                    Log.d("RAWVALUE ->>>", rawValue)

                    CoroutineScope(Dispatchers.IO).launch {
                        //val apiCall = getRetrofit().create(ApiService::class.java).foodDatabase("parser?app_id=$appId&app_key=$appKey&upc=$rawValue")
                        val apiCall = getRetrofit(urlBaseDatabase).create(ApiService::class.java)
                            .foodDatabase("parser?session=40&app_id=$appIdDatabase&app_key=$appKeyDatabase&ingr=arroz&nutrition-type=cooking")
                        //&health=vegetarian
                        val apiCallBody = apiCall.body()
                        if (apiCall.isSuccessful) {
                            if (!apiCallBody?.listHints?.isEmpty()!!) {
                                apiCall2 =
                                    getRetrofit(urlBaseNutrition).create(ApiService::class.java)
                                        .foodAnalysis("nutrition-data?app_id=$appIdNutrition&app_key=$appKeyNutrition&nutrition-type=cooking&ingr=${apiCallBody.listHints.first().food.id}")
                                apiCall2Body = apiCall2.body()!!

                                runOnUiThread {
                                    if (apiCall.isSuccessful) {
                                        if (apiCall2.isSuccessful) {
                                            Log.d(
                                                "SUCCESS ->>>",
                                                "API CALL NUTRITION DATA SUCCESFUL"
                                            )
                                            if (apiCall2Body.healthLabels.contains("VEGAN")) {
                                                alertBuilder(
                                                    R.style.alertDialogPositive,
                                                    "${apiCallBody.listHints.first().food.label} is vegan"
                                                )
                                            } else {
                                                alertBuilder(
                                                    R.style.alertDialogNegative,
                                                    "${apiCallBody.listHints.first().food.label} is not vegan"
                                                )
                                            }
                                        } else {
                                            Log.e("PROBLEM ->>", "API CALL NOT SUCCESFUL")
                                        }
                                    }
                                }
                            }else{
                                alert = true
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("PROBLEM ->>>>>>", "BARCODE NOT RECOGNIZED")
            }

        if(alert){
            MaterialAlertDialogBuilder(this@NewItem, R.style.alertDialogInconclusive)
                .setTitle("Item not found")
                .setMessage("This item is not on our database")
                .setNeutralButton(resources.getString(R.string.close)) { dialog, which ->
                    // Respond to negative button press
                }
                .show()
        }
    }

    private fun alertBuilder(style: Int, message: String) {
        MaterialAlertDialogBuilder(this@NewItem, style)
            .setTitle("Save Item?")
            .setMessage(message)
            .setNeutralButton(resources.getString(R.string.no)) { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton(resources.getString(R.string.save)) { dialog, which ->
                //TODO get the data
            }
            .show()
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