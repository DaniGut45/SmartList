package com.example.smartlist.view

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.smartlist.R
import com.example.smartlist.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UserFragment : Fragment() {

    private lateinit var tvUsername: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        val logoutButton: Button = view.findViewById(R.id.btn_logout)
        tvUsername = view.findViewById(R.id.tv_username)

        // 🔐 Obtener ID del usuario actual
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val tvNumListas = view.findViewById<TextView>(R.id.tv_num_listas)

        if (userId != null) {
            val userDocRef = Firebase.firestore.collection("usuarios").document(userId)

            // Cargar nombre
            userDocRef.get()
                .addOnSuccessListener { document ->
                    val nombre = document.getString("nombre")
                    tvUsername.text = nombre ?: "Usuario desconocido"
                }
                .addOnFailureListener {
                    tvUsername.text = "Error al cargar usuario"
                }

            // Contar listas del usuario
            userDocRef.collection("listas")
                .get()
                .addOnSuccessListener { snapshot ->
                    val cantidadListas = snapshot.size()
                    tvNumListas.text = "Número de listas: $cantidadListas"
                }
                .addOnFailureListener {
                    tvNumListas.text = "Error al cargar número de listas"
                }
        } else {
            tvUsername.text = "No hay sesión activa"
            tvNumListas.text = ""
        }


        if (userId != null) {
            Firebase.firestore.collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val nombre = document.getString("nombre")
                    tvUsername.text = nombre ?: "Usuario desconocido"
                }
                .addOnFailureListener {
                    tvUsername.text = "Error al cargar usuario"
                }
        } else {
            tvUsername.text = "No hay sesión activa"
        }

        logoutButton.setOnClickListener {
            SessionManager.isLoggedIn = false
            FirebaseAuth.getInstance().signOut()

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, MainFragment())
                .commit()

            (activity as? MainActivity)?.updateBottomNavColors("home")
        }

        return view
    }
}
