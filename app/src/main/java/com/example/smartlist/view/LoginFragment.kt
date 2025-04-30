// Paquete y dependencias necesarias
package com.example.smartlist.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
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

// Fragmento de login (pantalla de inicio de sesión)
class LoginFragment : Fragment() {

    // Declaración de variables necesarias
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordText: TextView
    private var remainingAttempts = 5 // Intentos de login permitidos
    private val RC_GOOGLE_SIGN_IN = 1002 // Código de respuesta para login con Google

    // Método que se ejecuta al crear la vista
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Inicialización de FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Configuración de inicio de sesión con Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Asignación de vistas
        forgotPasswordText = view.findViewById(R.id.tv_forgot_password)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        loginButton = view.findViewById(R.id.btn_login)
        val toRegisterText = view.findViewById<TextView>(R.id.tv_to_register)
        val btnGoogleLogin = view.findViewById<TextView>(R.id.btn_google_register)
        val ivTogglePassword = view.findViewById<ImageView>(R.id.iv_toggle_password)
        var isPasswordVisible = false

        // Alternar visibilidad de la contraseña
        ivTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                // Ocultar contraseña
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view)
            } else {
                // Mostrar contraseña
                etPassword.inputType = InputType.TYPE_CLASS_TEXT
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            }
            etPassword.setSelection(etPassword.text.length)
            isPasswordVisible = !isPasswordVisible
        }

        // Manejar "¿Olvidaste tu contraseña?"
        forgotPasswordText.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Introduce tu correo para recuperar la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Intento de login falso para forzar validación del correo
            auth.signInWithEmailAndPassword(email, "contrasena_incorrecta")
                .addOnCompleteListener { task ->
                    if (task.exception is FirebaseAuthInvalidUserException) {
                        Toast.makeText(requireContext(), "El correo no está registrado.", Toast.LENGTH_SHORT).show()
                    } else {
                        // El correo existe (aunque la contraseña esté mal)
                        auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Te hemos enviado un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Error al enviar el correo: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
        }

        // Botón de login tradicional (email y contraseña)
        loginButton.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Introduce todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Intentar iniciar sesión
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    SessionManager.isLoggedIn = true

                    // Cambiar a la pantalla principal si el login es exitoso
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
                    handleFailedLogin(exception)
                }
        }

        // Botón de login con Google
        btnGoogleLogin.setOnClickListener {
            // Cerrar sesión previa para forzar nueva autenticación
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
            }
        }

        // Navegar a la pantalla de registro
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

    // Manejar fallos en el login
    private fun handleFailedLogin(exception: Exception) {
        remainingAttempts--

        if (remainingAttempts > 0) {
            Toast.makeText(
                requireContext(),
                "Inicio de sesión fallido. Te quedan $remainingAttempts intentos.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            showBlockDialog()
        }
    }

    // Mostrar diálogo cuando la cuenta está temporalmente bloqueada
    private fun showBlockDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cuenta bloqueada")
        builder.setMessage("Se ha bloqueado temporalmente el inicio de sesión")
        builder.setPositiveButton("Restaurar contraseña") { _, _ ->
            forgotPasswordText.performClick()
        }
        builder.setNegativeButton("Aceptar") { dialog, _ ->
            blockLoginButton()
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.show()
    }

    // Bloquear botón de login durante 60 segundos
    private fun blockLoginButton() {
        loginButton.isEnabled = false
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                loginButton.text = "Bloqueado (${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                loginButton.isEnabled = true
                loginButton.text = "Iniciar sesión"
                remainingAttempts = 5
            }
        }.start()
    }

    // Resultado del intento de login con Google
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                // Autenticación con Firebase usando credenciales de Google
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid ?: return@addOnCompleteListener

                            // Guardar datos básicos del usuario en Firestore
                            val userMap = mapOf(
                                "nombre" to (user.displayName ?: "Sin nombre"),
                                "email" to (user.email ?: "Sin email")
                            )

                            Firebase.firestore.collection("usuarios").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    SessionManager.isLoggedIn = true

                                    // Ir a la pantalla principal
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
                val message = if (e.statusCode == 7) {
                    "Sin conexión a Internet. Por favor, verifica tu red e inténtalo de nuevo."
                } else {
                    "Error: ${e.message}"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
