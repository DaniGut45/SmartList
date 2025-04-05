// MainActivity.kt

package com.example.smartlist.view

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.smartlist.R
import com.example.smartlist.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var btnHome: ImageButton
    private lateinit var btnProfile: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace<MainFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors(true)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragmentContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top - 80, systemBars.right, systemBars.bottom)
            insets
        }

        btnHome.setOnClickListener {
            supportFragmentManager.commit {
                replace<MainFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors(true)
        }

        btnProfile.setOnClickListener {
            val destination = if (SessionManager.isLoggedIn) {
                UserFragment::class.java
            } else {
                LoginFragment::class.java
            }

            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, destination, null)
            }
            updateBottomNavColors(false)
        }
    }

    private fun updateBottomNavColors(isHomeSelected: Boolean) {
        btnHome.setColorFilter(getColor(if (isHomeSelected) R.color.dark_goldenrod else R.color.smoky_black))
        btnProfile.setColorFilter(getColor(if (!isHomeSelected) R.color.dark_goldenrod else R.color.smoky_black))
    }
}
