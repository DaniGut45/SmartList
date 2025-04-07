package com.example.smartlist.view

import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlist.R
import com.example.smartlist.model.Producto
import com.example.smartlist.model.ShoppingList
import com.example.smartlist.view.adapters.ListAdapter

class MainFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ListAdapter
    private val listas = mutableListOf<ShoppingList>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        recyclerView = view.findViewById(R.id.recyclerLists)

        // Inicializamos el adaptador con la lista
        adapter = ListAdapter(listas)
        recyclerView.adapter = adapter


        val viewModel = (activity as MainActivity).shoppingListViewModel

        viewModel.shoppingLists.observe(viewLifecycleOwner) { newList ->
            listas.clear()
            listas.addAll(newList)
            adapter.notifyDataSetChanged()
        }

        // ⚠️ Datos de prueba (puedes quitarlos luego)
        listas.add(
            ShoppingList(
                dateTime = "Hoy - 14:30",
                storeName = "Mercadona",
                products = listOf(
                    Producto("Tomates", 2, 1.30),
                    Producto("Pan", 1, 1.20)
                )
            )
        )

        listas.add(
            ShoppingList(
                dateTime = "Hoy - 15:10",
                storeName = "Carrefour",
                products = listOf(
                    Producto("Leche", 3, 1.10)
                )
            )
        )

        adapter.notifyDataSetChanged()

        // Botón para ir a crear lista
        view.findViewById<Button>(R.id.btn_create_list).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,  // enter
                    R.anim.slide_out_left,  // exit
                    R.anim.slide_in_left,   // popEnter (al volver)
                    R.anim.slide_out_right  // popExit (al volver)
                )
                .replace<CreateListFragment>(R.id.fragmentContainer)
                .addToBackStack(null)
                .commit()
        }


        return view
    }
}
