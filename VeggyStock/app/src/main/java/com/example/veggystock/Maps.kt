package com.example.veggystock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.veggystock.databinding.ActivityMapsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Maps : AppCompatActivity() {
    private lateinit var binding: ActivityMapsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMapsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        listeners()
    }

    private fun listeners() {
        binding.btnOptions.setOnClickListener {
            alertDialog()
        }
    }

    private fun alertDialog() {

        /*
        val gmmIntentUri = Uri.parse("geo:0,0?q=restaurants")
val gmmIntentUri =

  Uri.parse("geo:37.7749,-122.4194?z=10&q=restaurants")
  
  // Searches for 'Main Street' near San Francisco
val gmmIntentUri =
  Uri.parse("geo:37.7749,-122.4194?q=101+main+street")
val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
mapIntent.setPackage("com.google.android.apps.maps")
startActivity(mapIntent)
         */

        val singleItems = arrayOf("Vegan", "Vegetarian", "Gluten-Free")
        val checkedItem = 1

        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.select_option))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->

            }
            // Single-choice items (initialized with checked item)
            .setSingleChoiceItems(singleItems, checkedItem) { dialog, which ->
                // Respond to item chosen
            }
            .show()
    }
}