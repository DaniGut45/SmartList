// LoginFragment.kt
package com.example.smartlist.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.smartlist.R
import com.example.smartlist.utils.SessionManager

class LoginFragment : Fragment() {
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Este es el botón de "Iniciar sesión"
        val loginButton: Button = view.findViewById(R.id.btn_login)

        loginButton.setOnClickListener {
            SessionManager.isLoggedIn = true

            parentFragmentManager.commit {
                replace<MainFragment>(R.id.fragmentContainer)
            }
        }

        // Si tienes otro botón como el de Google, haz esto:
        // val googleButtonLayout: LinearLayout = view.findViewById(R.id.btn_google_layout)
        // googleButtonLayout.setOnClickListener { ... }

        return view
    }
}
