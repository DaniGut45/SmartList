package com.example.smartlist.view

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.smartlist.R
import com.example.smartlist.utils.SessionManager
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        auth = FirebaseAuth.getInstance()

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etUsername = view.findViewById<EditText>(R.id.et_username)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)
        val toLoginText = view.findViewById<TextView>(R.id.tv_to_login)
        val btnGoogleRegister = view.findViewById<TextView>(R.id.btn_google_register)

        // Botón de Google
        btnGoogleRegister.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
            }
        }

        // Registro tradicional con email y contraseña
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
                    val user = result.user ?: return@addOnSuccessListener

                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Verifica tu correo")
                                .setMessage("Te hemos enviado un correo a ${user.email}. Verifícalo y luego pulsa el botón para continuar.")
                                .setPositiveButton("He verificado") { _, _ ->
                                    user.reload().addOnCompleteListener { reloadTask ->
                                        if (reloadTask.isSuccessful && user.isEmailVerified) {
                                            // ✅ Verificado
                                            val userMap = mapOf(
                                                "nombre" to username,
                                                "email" to email
                                            )

                                            Firebase.firestore.collection("usuarios").document(user.uid).set(userMap)
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
                                        } else {
                                            // ❌ No verificado → eliminar cuenta
                                            user.delete().addOnCompleteListener {
                                                Toast.makeText(requireContext(), "Correo no verificado. Tu cuenta ha sido eliminada.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                                .setNegativeButton("Cancelar") { _, _ ->
                                    user.delete()
                                    Toast.makeText(requireContext(), "Registro cancelado. Cuenta eliminada.", Toast.LENGTH_SHORT).show()
                                }
                                .setCancelable(false)
                                .show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "No se pudo enviar el correo de verificación", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { exception ->
                    when (exception) {
                        is FirebaseAuthWeakPasswordException -> {
                            Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(requireContext(), "Este correo ya está registrado", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(requireContext(), "Correo no válido", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        // Navegar a login
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

    // Resultado del intent de Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid ?: return@addOnCompleteListener
                            val userMap = mapOf(
                                "nombre" to (user.displayName ?: "Sin nombre"),
                                "email" to (user.email ?: "Sin email")
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
                        } else {
                            Toast.makeText(requireContext(), "Error al iniciar sesión con Google", Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
