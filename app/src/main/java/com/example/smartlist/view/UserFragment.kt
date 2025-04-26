package com.example.smartlist.view

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
        val tvNumListas = view.findViewById<TextView>(R.id.tv_num_listas)

        // 🔐 Obtener ID del usuario actual (si hay sesión activa)
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val userDocRef = Firebase.firestore.collection("usuarios").document(userId)

            // Cargar nombre del usuario desde Firestore
            userDocRef.get()
                .addOnSuccessListener { document ->
                    val nombre = document.getString("nombre")
                    tvUsername.text = nombre ?: "Usuario desconocido"
                }
                .addOnFailureListener {
                    tvUsername.text = "Error al cargar usuario"
                }

            // Contar la cantidad de listas del usuario
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
            // Si no hay sesión, se muestra mensaje por defecto
            tvUsername.text = "No hay sesión activa"
            tvNumListas.text = ""
        }

        // Cierre de sesión y retorno al fragmento principal
        logoutButton.setOnClickListener {
            // Mostrar un AlertDialog de confirmación antes de cerrar sesión
            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí") { dialog, _ ->
                    // 🔴 1. Cerrar sesión en Firebase
                    FirebaseAuth.getInstance().signOut()

                    // 🔴 2. Marcar sesión como cerrada
                    SessionManager.isLoggedIn = false

                    // 🔴 3. Limpiar las listas en memoria
                    (activity as? MainActivity)?.shoppingListViewModel?.clear()

                    // 🔴 4. Volver al fragmento de registro (no al MainFragment)
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                        .replace(R.id.fragmentContainer, RegisterFragment())
                        .commit()

                    // 🔴 5. Actualizar colores de la barra inferior
                    (activity as? MainActivity)?.updateBottomNavColors("profile")

                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    // El usuario canceló, no hacemos nada
                    dialog.dismiss()
                }
                .show()
        }


        return view
    }
}
