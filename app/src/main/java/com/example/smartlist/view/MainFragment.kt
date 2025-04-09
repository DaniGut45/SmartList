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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class MainFragment : Fragment() {

    val userId = FirebaseAuth.getInstance().currentUser?.uid
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


        if (userId != null) {
            Firebase.firestore.collection("usuarios")
                .document(userId)
                .collection("listas")
                .get()
                .addOnSuccessListener { listasDocs ->
                    listas.clear()

                    listasDocs.forEach { doc ->
                        val dateTime = doc.getString("dateTime") ?: ""
                        val store = doc.getString("storeName") ?: ""
                        val total = doc.getDouble("total") ?: 0.0

                        // Traemos la subcolección de productos
                        doc.reference.collection("productos").get()
                            .addOnSuccessListener { productosDocs ->
                                val productos = productosDocs.map {
                                    Producto(
                                        it.getString("name") ?: "",
                                        (it.getLong("quantity") ?: 0).toInt(),
                                        it.getDouble("unitPrice") ?: 0.0
                                    )
                                }

                                val lista = ShoppingList(dateTime, store, productos, false, total)
                                listas.add(lista)
                                adapter.notifyDataSetChanged()
                            }
                    }
                }
        }
        return view
    }
}