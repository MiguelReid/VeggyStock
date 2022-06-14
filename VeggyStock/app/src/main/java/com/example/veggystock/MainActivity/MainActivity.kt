package com.example.veggystock.MainActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.veggystock.Items.Items
import com.example.veggystock.R
import com.example.veggystock.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.internal.AccountType
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var email = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        listener()
        checkSession()
        binding.btnGithub.setImageResource(R.drawable.github)
        binding.btnStack.setImageResource(R.drawable.stack)
    }

    /**
     * Codigo de boiler plate para iniciar
     * sesion con firebase authenticator
     */

    private val responseLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val task =
                    GoogleSignIn.getSignedInAccountFromIntent(it.data)
                try {
                    val account =
                        task.getResult(ApiException::class.java)
                    if (account != null) {
                        val credentials = GoogleAuthProvider.getCredential(
                            account.idToken,
                            null
                        )
                        FirebaseAuth.getInstance().signInWithCredential(credentials)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    startApp(account.email ?: "")
                                    Snackbar.make(
                                        binding.root,
                                        R.string.signed_in,
                                        Snackbar.LENGTH_SHORT
                                    )
                                        .show()
                                } else {
                                    Snackbar.make(
                                        binding.root,
                                        R.string.not_signed_in,
                                        Snackbar.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }
                    }
                } catch (e: ApiException) {
                    Log.e("ERROR ->>", " Google validation error" + e.localizedMessage!!.toString())
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.not_signed_in,
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            }
        }

    /**
     * Listeners para controlar la accion
     * a realizar cuando pulsamos cada boton
     */

    private fun listener() {
        binding.btnLogin.setOnClickListener {
            signIn()
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

    /**
     * The deja loggearte en la aplicacion
     * con los elementos necesarios
     */

    private fun signIn() {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("839047811149-aaft2k41468q5kei1u2s2ltunavho5i3.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(this, gso)
        googleClient.signOut()
        responseLauncher.launch(googleClient.signInIntent)
    }

    /**
     * Empieza un activity mandando el email
     * que se necesita para toda accion con
     * realtime database
     * @param email
     */

    private fun startApp(email: String) {
        val i = Intent(this, Items::class.java).apply {
            putExtra("EMAIL", email)
            putExtra("PROVIDERS", AccountType.GOOGLE)
        }
        startActivity(i)
    }

    /**
     * Comprueba si los valores estan bien introducidos
     */

    private fun checkSession() {
        val provider = AccountType.GOOGLE
        if (email.isNotEmpty() && provider.isNotEmpty())
            startApp(email)
    }
}