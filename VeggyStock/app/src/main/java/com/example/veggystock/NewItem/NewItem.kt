package com.example.veggystock.NewItem

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import androidx.core.net.toUri
import com.example.veggystock.Items.Items
import com.example.veggystock.R
import com.example.veggystock.databinding.ActivityNewItemBinding
import com.example.veggystock.foodDatabase.ApiService
import com.example.veggystock.foodDatabase.Gson2
import com.example.veggystock.foodDatabase.Hints
import com.example.veggystock.modelDB.Body
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewItem : AppCompatActivity() {

    private var cameraCode: Int = 666
    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var storage: StorageReference
    lateinit var binding: ActivityNewItemBinding
    private lateinit var imageUri: Uri
    private lateinit var imageBitmap: Bitmap
    private var urlBaseDatabase = "https://api.edamam.com/api/food-database/v2/"
    private var appIdDatabase = "f92aec81"
    private var appKeyDatabase = "0efe25b2bec1d420f5f78f7deaa3358c"
    private var urlBaseNutrition = "https://api.edamam.com/api/"
    private var appIdNutrition = "d1e4b94c"
    private var appKeyNutrition = "f461b422fb6f8acec69fe9b7badc15d8"
    lateinit var apiCall2: Response<Gson2>
    lateinit var apiCall2Body: Gson2
    lateinit var apiCallBody: Hints
    private var urlBaseUpc = "https://api.edamam.com/api/food-database/v2/"
    private var vegan = false
    val items = Items()

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

    /**
     * Codigo de boiler plate
     * que inicializa retrofit
     * @param urlBase
     * @return
     */

    private fun getRetrofit(urlBase: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(urlBase)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Checkea los campos si estan vacios para
     * no permitir si no guardar un item
     */

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

    /**
     * Indica que metodos hay que ejecutar
     * dependiendo del item en el menu pulsado
     */

    private fun menu() {
        binding.topBarNewItem?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.scan_item -> {
                    requestPermission()
                    true
                }
                R.id.search_database -> {
                    searchDatabase()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Busca en la base de datos por el nombre introducido en el campo nombre
     */

    private fun searchDatabase() {
        val name = binding.inputName?.editText?.text.toString()
        if (name.isNotEmpty()) {
            binding.inputName?.isErrorEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                // Se ejecuta en la corrutina confinada al hilo principal para que no
                // de error -> E/AndroidRuntime: FATAL EXCEPTION: DefaultDispatcher-worker-4
                val apiCall = getRetrofit(urlBaseDatabase).create(ApiService::class.java)
                    .foodDatabase("parser?session=40&app_id=$appIdDatabase&app_key=$appKeyDatabase&ingr=$name&nutrition-type=cooking")
                //&health=vegetarian
                if (apiCall.isSuccessful) {
                    // Controlamos pasarle el body solo si hay contenido
                    apiCallBody = apiCall.body()!!
                    if (apiCallBody.listHints.isNotEmpty()) {
                        // Solo si hay un resultado usaremos el foodId para llamar a la segunda API
                        apiCall2 =
                            getRetrofit(urlBaseNutrition).create(ApiService::class.java)
                                .foodAnalysis("nutrition-data?app_id=$appIdNutrition&app_key=$appKeyNutrition&nutrition-type=cooking&ingr=${apiCallBody.listHints.first().food.id}")
                        uiThread()
                        //Controlamos el alertDialog dependiendo de si es vegano o no
                    } else {
                        alertNotFound()
                    }
                }
            }
        } else if (name == "") {
            binding.inputName?.error = "A name is required"
        }
    }

    /**
     * Controla el caso de que el alimento sea o no vegano
     */

    private fun uiThread() {
        runOnUiThread {
            if (apiCall2.isSuccessful) {
                apiCall2Body = apiCall2.body()!!
                if (apiCall2Body.healthLabels.contains("VEGAN")) {
                    alertBuilder(
                        R.style.alertDialogPositive,
                        "${
                            apiCallBody.listHints.first().food.label.replaceFirstChar(
                                Char::titlecase
                            )
                        } is Vegan", true
                    )
                } else {
                    alertBuilder(
                        R.style.alertDialogNegative,
                        "${
                            apiCallBody.listHints.first().food.label.replaceFirstChar(
                                Char::titlecase
                            )
                        } is not Vegan", false
                    )
                }
            } else {
                Log.e("PROBLEM ->>", "API CALL NOT SUCCESFUL")
            }
        }
    }

    /**
     * Metodo para alertDialog si no se encuentra el alimento en la API
     * Se pone aparte para sintetizar el otro metodo y hacerlo mas legible
     */

    private fun alertNotFound() {
        runOnUiThread {
            MaterialAlertDialogBuilder(
                this@NewItem,
                R.style.alertDialogInconclusive
            )
                .setTitle("Item not found")
                .setMessage("This item is not on our database")
                .setNeutralButton(resources.getString(R.string.close)) { _, _ ->
                }
                .show()
        }
    }

    /**
     * Coge los datos para subirlos a realtimeDatabase
     * y me deja elegir foto / abrir camera para subir la imagen
     * a storage
     */

    private fun listener() {
        binding.btnSave.setOnClickListener {
            val name = binding.inputName?.editText?.text.toString()
            val provider = binding.inputProvider?.editText?.text.toString()
            val aux = binding.inputPrice?.editText?.text.toString()
            val price = aux.toFloat()
            val rating = binding.ratingBar.rating
            val address = binding.inputStreet?.editText?.text.toString()
            if (binding.checkVeggy?.isChecked == true) {
                vegan = true
            }
            saveItem(name, provider, price, rating, address, vegan)
            onBackPressed()
        }
        binding.imageButton.setOnClickListener {
            if (binding.switchCamera!!.isChecked) requestPermission()
            else chooseImage()
        }
    }

    /**
     * Pide permiso para acceder a la camara
     */

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

    /**
     * Si se da permiso a la camara me dejara
     * echar una foto
     * @param requestCode
     * @param permissions
     * @param grantResults
     */

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

    /**
     * Me abrira un intent del almacenamiento interno
     * para elegir una imagen a subir a storage
     */

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            intent,
            100
        )
    }

    /**
     * Guarda un item en relatimeDatabase y sube la imagen que hayamos
     * elegido / hecho / obtenido con la API a storage
     * @param name
     * @param provider
     * @param price
     * @param rating
     * @param address
     * @param vegan
     */

    private fun saveItem(
        name: String,
        provider: String,
        price: Float,
        rating: Float,
        address: String,
        vegan: Boolean
    ) {
        val fileName = "$name $provider $address"
        initDB()
        uploadImage()
        reference.child(fileName)
            .setValue(Body(name, provider, price, address, rating, false, vegan))
            .addOnSuccessListener {
                Snackbar.make(binding.root, R.string.item_saved, Snackbar.LENGTH_SHORT)
                    .show()
            }.addOnFailureListener {
                Snackbar.make(binding.root, R.string.item_not_saved, Snackbar.LENGTH_SHORT)
                    .show()
            }
    }

    /**
     * A partir de la imagen echada con la camara escanea un codigo de barras para
     * mandar el valor crudo (raw) a la API de Edamam
     * @param bitmap
     */

    private fun scanBarcodes(bitmap: Bitmap) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13
            ).build()
        // Solo permito estas opciones ya que son las aceptadas en la API Edamam

        val scanner = BarcodeScanning.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)
        // Asi puedo usar la imagen echada con la camara que es un bitmap

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue.toString()
                    Log.d("RAWVALUE ->>>", rawValue)

                    CoroutineScope(IO).launch {
                        val apiCall = getRetrofit(urlBaseUpc).create(ApiService::class.java)
                            .foodDatabase("parser?app_id=$appIdDatabase&app_key=$appKeyDatabase&upc=$rawValue")
                        // Mandamos a la primera api el valor UPC/EAN
                        if (apiCall.isSuccessful) {
                            // Si es exitoso cogemos el contenido
                            apiCallBody = apiCall.body()!!
                            if (apiCallBody.listHints.isNotEmpty()) {
                                // Si se ha encontrado el producto (diferente a llamada no exitosa)
                                // Llamamos a la segunda api
                                apiCall2 =
                                    getRetrofit(urlBaseNutrition).create(ApiService::class.java)
                                        .foodAnalysis("nutrition-data?app_id=$appIdNutrition&app_key=$appKeyNutrition&ingr=${apiCallBody.listHints.first().food.id}")
                                // Cogemos el valor vegan y respondemos de forma diferente dependiendo a si es o no
                                uiThread()
                            } else {
                                alertNotFound()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("ERROR ->>", "BARCODE NOT RECOGNIZED")
            }
    }

    /**
     * Para poder reutilizar codigo usamos el mismo para respuesta vegana y no vegana
     * Le mandamos el id del estilo (rojo o verde) y si es vegano o no para
     * indicarlo en el checkBox
     * @param style
     * @param message
     * @param veggy
     */

    private fun alertBuilder(style: Int, message: String, veggy: Boolean) {
        MaterialAlertDialogBuilder(this@NewItem, style)
            .setTitle("Save Item?")
            .setMessage(message)
            .setNeutralButton(resources.getString(R.string.close)) { _, _ -> }
            .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            .setPositiveButton(resources.getString(R.string.save)) { _, _ ->
                setData(veggy)
            }
            .show()
    }

    /**
     * Necesitamos pasar el url a bitmap para escribirlo en un fichero y
     * pasarlo a uri para ser capaces de subir la imagen a storage, ya que
     * el sdk no permite subir una url de imagen
     * @param url
     */

    private fun urlToBitmap(url: Uri) {
        Picasso.get().load(url).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                imageUri = items.bitmapToUri(bitmap!!, cacheDir)
                Log.d("INFO LOADED BITMAP->>", imageUri.toString())
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}
        })
    }

    /**
     * Autocompletamos campos gracias a la api y si ese item no tiene
     * imagen asignamos un valor de imagen negra para subir a storage
     * @param veggy
     */

    private fun setData(veggy: Boolean) {
        binding.inputName?.editText?.setText(apiCallBody.listHints.first().food.label)
        Picasso.get().load(apiCallBody.listHints.first().food.image).fit()
            .into(binding.imageButton)
        binding.checkVeggy?.isChecked = veggy

        val imageUriAux = apiCallBody.listHints.first().food?.image
        if (!imageUriAux.isNullOrBlank()) {
            val imageUriRaw = imageUriAux?.toUri()
            urlToBitmap(imageUriRaw!!)
        } else {
            val imageUriRaw = "https://img.icons8.com/ios-filled/344/no-image.png"
            urlToBitmap(imageUriRaw.toUri())
        }
    }

    /**
     * Si se aceptan los permisos
     * Dos codigos uno para echar una foto / escanear
     * y otro para coger la data del almacenamiento interno
     * @param requestCode
     * @param resultCode
     * @param data
     */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            imageUri = data?.data!!
            Picasso.get()!!.load(imageUri).fit().into(binding.imageButton)
        }

        if (requestCode == cameraCode && resultCode == RESULT_OK) {
            imageBitmap = data?.extras?.get("data") as Bitmap
            if (binding.switchCamera?.isChecked == true) {
                imageUri = items.bitmapToUri(imageBitmap, cacheDir)
                Picasso.get().load(imageUri).fit().into(binding.imageButton)
            } else {
                scanBarcodes(imageBitmap)
            }
        }
    }

    /**
     * Subimos la imagen a storage con el mismo nombre que el
     * nodo de realtime database
     */

    private fun uploadImage() {
        val name = binding.inputName?.editText?.text.toString()
        val provider = binding.inputProvider?.editText?.text.toString()
        val address = binding.inputStreet?.editText?.text.toString()
        val fileName = "$name $provider $address"
        //Log.d("INFO IMAGE URI UPLOAD->>", imageUri.toString())
        storage = FirebaseStorage.getInstance().getReference("images/$fileName")

        storage.putFile(imageUri).addOnSuccessListener {
            binding.imageButton.setImageURI(null)
        }.addOnFailureListener {
            Toast.makeText(this@NewItem, "Image Not Uploaded", Toast.LENGTH_SHORT).show()
        }

    }

    /*
    fun sendData(): HashMap<String, String> {
        val map = HashMap<String, String>()
        map["FILENAME"] = fileName
        Log.i("INFO FILENAME ->>", fileName)
        return map
    }
     */

    /**
     * Inicializamos la base de datos
     */

    private fun initDB() {
        val intent = intent
        val email = intent.getStringExtra("EMAIL")
        db =
            FirebaseDatabase.getInstance("https://veggystock-default-rtdb.europe-west1.firebasedatabase.app/")
        reference = db.getReference("Users").child(email.toString())
    }
}