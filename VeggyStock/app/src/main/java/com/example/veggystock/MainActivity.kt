package com.example.veggystock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.veggystock.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.internal.AccountType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var email = ""

    private val responseLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val task =
                    GoogleSignIn.getSignedInAccountFromIntent(it.data)
                try {
                    val cuenta =
                        task.getResult(ApiException::class.java)
                    if (cuenta != null) {
                        val credenciales = GoogleAuthProvider.getCredential(
                            cuenta.idToken,
                            null
                        )
                        FirebaseAuth.getInstance().signInWithCredential(credenciales)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    irApp(cuenta.email ?: "")
                                    Toast.makeText(this, "You signed in!", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    mostrarError()
                                }
                            }
                    }
                } catch (e: ApiException) {
                    Log.d("Error al validar con Google", e.localizedMessage.toString())
                }
            } else {
                Toast.makeText(this, "ERROR AL INICIAR", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ponerListener()
        comprobarSesion()
        binding.btnGithub.setImageResource(R.drawable.github)
        binding.btnStack.setImageResource(R.drawable.stack)
    }

    private fun ponerListener() {
        binding.btnLogin.setOnClickListener {
            iniciarSesion()
        }
        binding.btnGithub.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MiguelReid"))
            startActivity(i)
        }
        binding.btnStack.setOnClickListener {
            val i = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://stackoverflow.com/users/14751023/miguel-reid-ruiz")
            )
            startActivity(i)
        }
    }

    private fun iniciarSesion() {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("839047811149-aaft2k41468q5kei1u2s2ltunavho5i3.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(this, gso)
        googleClient.signOut()
        responseLauncher.launch(googleClient.signInIntent)
    }

    private fun irApp(email: String) {
        val i = Intent(this, Items::class.java).apply {
            putExtra("EMAIL", email)
            putExtra("PROVIDERS", AccountType.GOOGLE)
        }
        startActivity(i)
    }

    private fun mostrarError() {
        val alerta =
            AlertDialog.Builder(this).setTitle("Error").setMessage("Couldn't Sign In")
                .setPositiveButton("Aceptar", null)
                .create().show()

    }

    private fun comprobarSesion() {
        val provider = AccountType.GOOGLE

        if (email.isNotEmpty() && provider.isNotEmpty())
            irApp(email)
    }
}