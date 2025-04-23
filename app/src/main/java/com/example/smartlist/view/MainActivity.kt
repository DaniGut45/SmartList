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
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var btnHome: ImageButton
    private lateinit var btnProfile: ImageButton

    // ViewModel compartido para manejar datos de la lista de compras
    val shoppingListViewModel by viewModels<ShoppingListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Evita que el tema splash se quede permanentemente aplicado
        setTheme(R.style.Theme_SmartList)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)

        val addButton: ImageButton = findViewById(R.id.btn_add)
        addButton.setOnClickListener {
            // Reemplaza el fragmento actual con el de creación de lista
            supportFragmentManager.commit {
                replace<CreateListFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors("add")
        }

        // Carga inicial de fragmento dependiendo del estado de sesión
        if (savedInstanceState == null) {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                SessionManager.isLoggedIn = true
                supportFragmentManager.commit {
                    replace<MainFragment>(R.id.fragmentContainer)
                }
                updateBottomNavColors("home")
            } else {
                SessionManager.isLoggedIn = false
                supportFragmentManager.commit {
                    replace<RegisterFragment>(R.id.fragmentContainer)
                }
                updateBottomNavColors("profile")
            }
        }

        // Ajusta padding del contenedor según la visibilidad de las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragmentContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top - 80, systemBars.right, systemBars.bottom)
            insets
        }

        // Navega a Home (MainFragment)
        btnHome.setOnClickListener {
            supportFragmentManager.commit {
                replace<MainFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors("home")
        }

        // Navega a perfil o registro dependiendo del estado de sesión
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

    // Cambia el color de los botones de navegación inferior según el ítem seleccionado
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
