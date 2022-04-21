package com.example.veggystock

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.veggystock.databinding.ActivityFragmentManagerBinding

class FragmentManager : AppCompatActivity() {
    lateinit var binding: ActivityFragmentManagerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityFragmentManagerBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        listeners()
    }

    private fun listeners() {
        binding.btnBlog1.setOnClickListener {
            replaceFragment(Fragment1())
        }
        binding.btnBlog2.setOnClickListener {
            replaceFragment(Fragment2())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()
    }
}