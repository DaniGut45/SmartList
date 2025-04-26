package com.example.smartlist.view

// Importaciones necesarias para trabajar con Firebase, Google Sign-In y Android
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

// Fragmento que gestiona el inicio de sesión de los usuarios
class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth // Objeto de autenticación Firebase
    private lateinit var googleSignInClient: GoogleSignInClient // Cliente para inicio de sesión con Google
    private val RC_GOOGLE_SIGN_IN = 1002 // Código de solicitud para identificar el login con Google

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        auth = FirebaseAuth.getInstance() // Inicializamos la autenticación Firebase

        // Configuración de opciones para Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // ID de cliente OAuth 2.0
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso) // Inicializamos cliente de Google

        // Referencias a los elementos de la vista
        val forgotPasswordText = view.findViewById<TextView>(R.id.tv_forgot_password)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val loginButton = view.findViewById<Button>(R.id.btn_login)
        val toRegisterText = view.findViewById<TextView>(R.id.tv_to_register)
        val btnGoogleLogin = view.findViewById<TextView>(R.id.btn_google_register)

        // 📧 Recuperación de contraseña por correo
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

        // 🔒 Inicio de sesión con email y contraseña
        loginButton.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Introduce todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    SessionManager.isLoggedIn = true // Usuario logueado correctamente

                    // Navegar al fragmento principal tras login exitoso
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                        .replace(R.id.fragmentContainer, MainFragment())
                        .commit()

                    (activity as? MainActivity)?.updateBottomNavColors("home") // Cambia colores de navegación
                }
                .addOnFailureListener { exception ->
                    // Manejo de errores específicos de Firebase
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

        // 🔵 Inicio de sesión con cuenta de Google
        btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                // Abrimos el intent de Google Sign-In
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
            }
        }

        // ➡️ Enlace para ir al registro de nuevo usuario
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

    // 📥 Manejo del resultado del intent de Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                // Autenticamos en Firebase usando las credenciales de Google
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid ?: return@addOnCompleteListener

                            val userMap = mapOf(
                                "nombre" to (user.displayName ?: "Sin nombre"),
                                "email" to (user.email ?: "Sin email")
                            )

                            // Guardamos datos básicos del usuario en Firestore
                            Firebase.firestore.collection("usuarios").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    SessionManager.isLoggedIn = true

                                    // Navegamos al fragmento principal después del login
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
