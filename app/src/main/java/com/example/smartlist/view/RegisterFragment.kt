package com.example.smartlist.view

import android.content.Intent
import android.os.Bundle
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
import androidx.core.content.ContextCompat

class RegisterFragment : Fragment() {

    // Firebase Auth y cliente de Google Sign-In
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 1001 // Código de resultado para el intent de Google

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        auth = FirebaseAuth.getInstance() // Inicializa Firebase Authentication

        // Configura Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Referencias a elementos de UI
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etUsername = view.findViewById<EditText>(R.id.et_username)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)
        val toLoginText = view.findViewById<TextView>(R.id.tv_to_login)
        val btnGoogleRegister = view.findViewById<TextView>(R.id.btn_google_register)
        val ivTogglePassword = view.findViewById<ImageView>(R.id.iv_toggle_password)
        val tvPasswordStrength = view.findViewById<TextView>(R.id.tv_password_strength)

        // Botón para registrar con Google
        btnGoogleRegister.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
            }
        }

        // Mostrar / ocultar contraseña al pulsar el icono
        var isPasswordVisible = false
        ivTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            }
            etPassword.setSelection(etPassword.text.length)
            isPasswordVisible = !isPasswordVisible
        }

        // Cambia texto y color del indicador de fuerza de contraseña
        etPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val password = s.toString()
                val strength = getPasswordStrength(password)
                tvPasswordStrength.text = strength

                when (strength) {
                    "Débil" -> tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    "Aceptable" -> tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                    "Fuerte" -> tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                }
            }
        })

        // Registro de usuario
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validación de campos vacíos
            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación de correo válido
            if (!isEmailValid(email)) {
                etEmail.error = "Introduce un correo válido de Gmail, Hotmail, Outlook, Yahoo o Protonmail."
                return@setOnClickListener
            }

            // Validación de contraseña segura
            if (!isPasswordValid(password)) {
                etPassword.error = "La contraseña debe tener al menos 7 caracteres, una mayúscula, una minúscula y un número."
                return@setOnClickListener
            }

            // Registrar en Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user ?: return@addOnSuccessListener
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            // Diálogo para confirmar verificación
                            AlertDialog.Builder(requireContext())
                                .setTitle("Verifica tu correo")
                                .setMessage("Te hemos enviado un correo a ${user.email}. Verifícalo y luego pulsa el botón para continuar.")
                                .setPositiveButton("He verificado") { _, _ ->
                                    user.reload().addOnCompleteListener { reloadTask ->
                                        if (reloadTask.isSuccessful && user.isEmailVerified) {
                                            val userMap = mapOf("nombre" to username, "email" to email)
                                            Firebase.firestore.collection("usuarios").document(user.uid).set(userMap)
                                                .addOnSuccessListener {
                                                    SessionManager.isLoggedIn = true
                                                    parentFragmentManager.beginTransaction()
                                                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                                        .replace(R.id.fragmentContainer, MainFragment())
                                                        .commit()
                                                    (activity as? MainActivity)?.updateBottomNavColors("home")
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(requireContext(), "Error al guardar usuario", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
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
                        is FirebaseAuthWeakPasswordException -> Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                        is FirebaseAuthUserCollisionException -> Toast.makeText(requireContext(), "Este correo ya está registrado", Toast.LENGTH_SHORT).show()
                        is FirebaseAuthInvalidCredentialsException -> Toast.makeText(requireContext(), "Correo no válido", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Botón para ir a login
        toLoginText.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.fragmentContainer, LoginFragment())
                .commit()
        }

        return view
    }

    // Verifica si el correo tiene dominio permitido
    private fun isEmailValid(email: String): Boolean {
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        val allowedDomains = listOf("gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "protonmail.com")
        if (!emailPattern.matcher(email).matches()) return false
        val domain = email.substringAfter("@").lowercase()
        return allowedDomains.contains(domain)
    }

    // Valida contraseña con al menos 7 caracteres, mayúscula, minúscula y número
    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{7,}")
        return password.matches(passwordPattern)
    }

    // Devuelve un string indicando la fuerza de la contraseña
    private fun getPasswordStrength(password: String): String {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val length = password.length

        if (!(hasUppercase && hasLowercase && hasDigit)) return "Débil"
        return when {
            length >= 10 -> "Fuerte"
            length == 9 -> "Aceptable"
            length in 7..8 -> "Débil"
            else -> "Débil"
        }
    }

    // Resultado de login con Google
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
                                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
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
