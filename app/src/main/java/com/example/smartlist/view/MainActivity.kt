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
import com.example.smartlist.view.MainFragment

class MainActivity : AppCompatActivity() {

    private lateinit var btnHome: ImageButton
    private lateinit var btnProfile: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)

        // Cargar el fragmento al iniciar la actividad
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace<MainFragment>(R.id.fragmentContainer)
            }
            updateBottomNavColors(true) // Establece Home como el botón seleccionado al inicio
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragmentContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top - 80, systemBars.right, systemBars.bottom) // Asegúrate de ajustar el top según la altura de topBar
            insets
        }

        // Configurar botón de navegación para el Home
        btnHome.setOnClickListener {
            updateBottomNavColors(true) // Cambia a Home
        }

        // Configurar botón de navegación para el Perfil
        btnProfile.setOnClickListener {
            updateBottomNavColors(false) // Cambia a Perfil
        }
    }

    private fun updateBottomNavColors(isHomeSelected: Boolean) {
        if (isHomeSelected) {
            btnHome.setColorFilter(getColor(R.color.dark_goldenrod)) // Cambia el color de Home
            btnProfile.setColorFilter(getColor(R.color.smoky_black)) // Cambia el color de Perfil
        } else {
            btnHome.setColorFilter(getColor(R.color.smoky_black)) // Cambia el color de Home
            btnProfile.setColorFilter(getColor(R.color.dark_goldenrod)) // Cambia el color de Perfil
        }
    }
}
