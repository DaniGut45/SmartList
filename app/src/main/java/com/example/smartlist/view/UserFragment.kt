package com.example.smartlist.view

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

class UserFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        val logoutButton: Button = view.findViewById(R.id.btn_logout) // Asegúrate del ID

        logoutButton.setOnClickListener {
            SessionManager.isLoggedIn = false

            parentFragmentManager.commit {
                replace<MainFragment>(R.id.fragmentContainer)
            }
        }

        return view
    }
}
