package com.example.smartlist.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.smartlist.R
import com.example.smartlist.utils.SessionManager
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 1002 // diferente del de RegisterFragment

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val forgotPasswordText = view.findViewById<TextView>(R.id.tv_forgot_password)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val loginButton = view.findViewById<Button>(R.id.btn_login)
        val toRegisterText = view.findViewById<TextView>(R.id.tv_to_register)
        val btnGoogleLogin = view.findViewById<TextView>(R.id.btn_google_register) // Usa el mismo ID si el botón es compartido

        forgotPasswordText.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Introduce tu correo para recuperar la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Te hemos enviado un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al enviar el correo: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        loginButton.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Introduce todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
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
                .addOnFailureListener { exception ->
                    when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            Toast.makeText(requireContext(), "No existe una cuenta con este correo", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(requireContext(), "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

        }

        btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
            }
        }

        toRegisterText.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, RegisterFragment())
                .commit()
        }

        return view
    }

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
