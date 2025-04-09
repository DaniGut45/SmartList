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

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        auth = FirebaseAuth.getInstance()

        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etUsername = view.findViewById<EditText>(R.id.et_username)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)
        val toLoginText = view.findViewById<TextView>(R.id.tv_to_login)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    // Guardar el nombre del usuario en Firestore
                    val userMap = mapOf(
                        "nombre" to username,
                        "email" to email
                    )

                    Firebase.firestore.collection("usuarios").document(userId).set(userMap)
                        .addOnSuccessListener {
                            SessionManager.isLoggedIn = true

                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                                )
                                .replace(R.id.fragmentContainer, MainFragment())
                                .commit()

                            (activity as? MainActivity)?.updateBottomNavColors("home")
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error al guardar usuario", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        toLoginText.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(R.id.fragmentContainer, LoginFragment())
                .commit()
        }

        return view
    }
}
