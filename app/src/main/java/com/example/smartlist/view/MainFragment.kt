package com.example.smartlist.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlist.R
import com.example.smartlist.model.ShoppingList
import com.example.smartlist.view.adapters.ListAdapter

class MainFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        recyclerView = view.findViewById(R.id.recyclerLists)

        val sampleData = listOf(
            ShoppingList("Hoy – 14:30", "Mercadona"),
            ShoppingList("Viernes, 6 de Agosto – 12:00", "Carrefour"),
            ShoppingList("Jueves, 5 de Agosto – 13:45", "Lidl"),
            ShoppingList("Miércoles, 4 de Agosto – 11:30", "Froiz")
        )

        adapter = ListAdapter(sampleData)
        recyclerView.adapter = adapter

        return view
    }
}
