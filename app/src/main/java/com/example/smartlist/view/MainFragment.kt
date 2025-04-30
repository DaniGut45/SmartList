package com.example.smartlist.view

// Importaciones necesarias
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.smartlist.R
import com.example.smartlist.model.Producto
import com.example.smartlist.model.ShoppingList
import com.example.smartlist.view.adapters.ListAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

// Fragmento principal que muestra las listas de la compra
class MainFragment : Fragment() {

    // Guarda el ID del usuario logueado en Firebase Auth
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // RecyclerView para mostrar la lista de compras
    private lateinit var recyclerView: RecyclerView

    // Adaptador del RecyclerView
    private lateinit var adapter: ListAdapter

    // Lista local donde almacenamos las listas de compras
    private val listas = mutableListOf<ShoppingList>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflamos el layout del fragment
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        // Inicializamos el RecyclerView y su adaptador
        recyclerView = view.findViewById(R.id.recyclerLists)
        adapter = ListAdapter(listas) { shoppingList ->
            mostrarDialogoConfirmacion(shoppingList)
        }

        recyclerView.adapter = adapter

        // Obtenemos el ViewModel compartido con la MainActivity
        val viewModel = (activity as MainActivity).shoppingListViewModel

        // Observamos los cambios en las listas de compra del ViewModel
        viewModel.shoppingLists.observe(viewLifecycleOwner) { newList ->
            listas.clear()
            listas.addAll(newList)
            adapter.notifyDataSetChanged()
        }

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadListsFromFirestore()
            viewModel.shoppingLists.observe(viewLifecycleOwner) {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // Pedimos al ViewModel que cargue las listas desde Firestore (Base de Datos)
        viewModel.loadListsFromFirestore()

        // Configuramos el botón de "Crear nueva lista"
        view.findViewById<Button>(R.id.btn_create_list).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace<CreateListFragment>(R.id.fragmentContainer) // Cambiamos al fragmento de crear lista
                .addToBackStack(null) // Guardamos esta transacción en la pila para poder volver atrás
                .commit()
        }

        return view
    }

    private fun mostrarDialogoConfirmacion(shoppingList: ShoppingList) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar lista")
            .setMessage("¿Estás seguro de que quieres eliminar esta lista?")
            .setPositiveButton("Sí") { _, _ ->
                eliminarListaDeFirestore(shoppingList)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarListaDeFirestore(shoppingList: ShoppingList) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore

        val listaRef = db.collection("usuarios")
            .document(userId)
            .collection("listas")
            .document(shoppingList.id)

        // Eliminar subcolección "productos" antes de eliminar la lista
        listaRef.collection("productos").get()
            .addOnSuccessListener { productos ->
                for (producto in productos) {
                    producto.reference.delete()
                }

                // Luego eliminamos la lista en sí
                listaRef.delete()
                    .addOnSuccessListener {
                        listas.remove(shoppingList)
                        adapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "Lista eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al eliminar lista: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al eliminar productos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


}
