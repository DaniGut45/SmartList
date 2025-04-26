package com.example.smartlist.view

// Importaciones necesarias
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageButton
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

// Actividad principal que gestiona la navegación y la sesión del usuario
class MainActivity : AppCompatActivity() {

    private lateinit var btnHome: ImageButton
    private lateinit var btnProfile: ImageButton

    // ViewModel compartido entre fragments para manejar las listas de compra
    val shoppingListViewModel by viewModels<ShoppingListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Eliminamos el tema splash para mostrar el tema principal
        setTheme(R.style.Theme_SmartList)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos botones de navegación inferior
        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)

        val addButton: ImageButton = findViewById(R.id.btn_add)
        addButton.setOnClickListener {
            // Cuando se pulsa el botón de añadir, abre el fragmento de creación de lista
            supportFragmentManager.commit {
                replace<CreateListFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors("add") // Actualiza colores de navegación
        }

        // 🚀 Carga inicial de fragmento según si el usuario ya está logueado o no
        if (savedInstanceState == null) {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // Si el usuario existe, marcamos sesión activa y mostramos el MainFragment
                SessionManager.isLoggedIn = true
                supportFragmentManager.commit {
                    replace<MainFragment>(R.id.fragmentContainer)
                }
                updateBottomNavColors("home")
            } else {
                // Si no hay usuario, mostramos el fragmento de registro
                SessionManager.isLoggedIn = false
                supportFragmentManager.commit {
                    replace<RegisterFragment>(R.id.fragmentContainer)
                }
                updateBottomNavColors("profile")
            }
        }

        // 📏 Ajuste del padding de la pantalla para respetar las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragmentContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top - 80, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔙 Pulsar el botón de Home → vuelve al MainFragment
        btnHome.setOnClickListener {
            supportFragmentManager.commit {
                replace<MainFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors("home")
        }

        // 👤 Pulsar el botón de Profile → va al fragmento de usuario o registro, según sesión
        btnProfile.setOnClickListener {
            val fragment = if (SessionManager.isLoggedIn) {
                UserFragment() // Usuario logueado
            } else {
                RegisterFragment() // Usuario no registrado
            }

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right, // Animación al entrar
                    R.anim.slide_out_left  // Animación al salir
                )
                .replace(R.id.fragmentContainer, fragment)
                .commit()

            updateBottomNavColors("profile")
        }
    }

    // 🎨 Función que actualiza los colores de los iconos de navegación inferior
    fun updateBottomNavColors(selected: String) {
        val golden = getColor(R.color.dark_goldenrod) // Color resaltado
        val black = getColor(R.color.smoky_black)      // Color base

        when (selected) {
            "home" -> {
                btnHome.setColorFilter(golden) // Resalta Home
                btnProfile.setColorFilter(black)
                findViewById<ImageButton>(R.id.btn_add).setColorFilter(black)
            }
            "add" -> {
                btnHome.setColorFilter(black)
                btnProfile.setColorFilter(black)
                findViewById<ImageButton>(R.id.btn_add).setColorFilter(golden) // Resalta botón de añadir
            }
            "profile" -> {
                btnHome.setColorFilter(black)
                btnProfile.setColorFilter(golden) // Resalta Perfil
                findViewById<ImageButton>(R.id.btn_add).setColorFilter(black)
            }
        }
    }
}
