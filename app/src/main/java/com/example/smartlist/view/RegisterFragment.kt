package com.example.smartlist.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.smartlist.R
import com.example.smartlist.utils.SessionManager

class RegisterFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        val registerButton: Button = view.findViewById(R.id.btn_register)
        val toLoginText: TextView = view.findViewById(R.id.tv_to_login)

        registerButton.setOnClickListener {
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



        toLoginText.setOnClickListener {
            parentFragmentManager.commit {
                replace<LoginFragment>(R.id.fragmentContainer)
            }
        }

        return view
    }
}
