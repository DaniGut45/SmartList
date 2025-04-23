package com.example.smartlist.utils

// Objeto singleton que gestiona el estado de sesión del usuario.
// `isLoggedIn` actúa como una bandera global para saber si el usuario ha iniciado sesión.
// Esto puede ser útil para mostrar diferentes pantallas o restringir funcionalidades según el estado.
object SessionManager {
    var isLoggedIn: Boolean = false
}
