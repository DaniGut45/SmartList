package com.example.smartlist.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.smartlist.R
import com.example.smartlist.utils.SessionManager
import com.example.smartlist.view.RegisterFragment
import com.example.smartlist.view.UserFragment
import com.example.smartlist.viewmodel.ShoppingListViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var btnHome: ImageButton
    private lateinit var btnProfile: ImageButton

    val shoppingListViewModel by viewModels<ShoppingListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // CAMBIA el tema antes de cargar el contenido para evitar usar el splash permanentemente
        setTheme(R.style.Theme_SmartList)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)

        val addButton: ImageButton = findViewById(R.id.btn_add)
        addButton.setOnClickListener {
            supportFragmentManager.commit {
                replace<CreateListFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors("add")
        }


        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace<MainFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors("home")
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
            updateBottomNavColors("home")
        }


        btnProfile.setOnClickListener {
            val fragment = if (SessionManager.isLoggedIn) {
                UserFragment()
            } else {
                RegisterFragment()
            }

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(R.id.fragmentContainer, fragment)
                .commit()

            updateBottomNavColors("profile")
        }




    }

    fun updateBottomNavColors(selected: String) {
        val golden = getColor(R.color.dark_goldenrod)
        val black = getColor(R.color.smoky_black)

        when (selected) {
            "home" -> {
                btnHome.setColorFilter(golden)
                btnProfile.setColorFilter(black)
                findViewById<ImageButton>(R.id.btn_add).setColorFilter(black)
            }
            "add" -> {
                btnHome.setColorFilter(black)
                btnProfile.setColorFilter(black)
                findViewById<ImageButton>(R.id.btn_add).setColorFilter(golden)
            }
            "profile" -> {
                btnHome.setColorFilter(black)
                btnProfile.setColorFilter(golden)
                findViewById<ImageButton>(R.id.btn_add).setColorFilter(black)
            }
        }
    }

}
