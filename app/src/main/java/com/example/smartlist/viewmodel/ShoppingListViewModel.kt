package com.example.smartlist.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartlist.model.Producto
import com.example.smartlist.model.ShoppingList
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class ShoppingListViewModel : ViewModel() {

    private val _shoppingLists = MutableLiveData<MutableList<ShoppingList>>(mutableListOf())
    val shoppingLists: LiveData<MutableList<ShoppingList>> = _shoppingLists

    // Añade una nueva lista con los productos seleccionados
    fun addList(supermarket: String, productos: List<Producto>) {
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val now = sdf.format(Date())
        val total = productos.sumOf { it.unitPrice * it.quantity }

        val list = ShoppingList(
            dateTime = now,
            storeName = supermarket,
            products = productos,
            total = total
        )

        // Se actualiza la lista observable para notificar a la vista
        _shoppingLists.value?.add(list)
        _shoppingLists.postValue(_shoppingLists.value)

        // Guardar en Firestore si el usuario está autenticado
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore

        val listData = mapOf(
            "dateTime" to now,
            "storeName" to supermarket,
            "total" to total
        )

        db.collection("usuarios").document(userId)
            .collection("listas").add(listData)
            .addOnSuccessListener { docRef ->
                // Subcolección de productos dentro de la lista
                productos.forEach { producto ->
                    val productoMap = mapOf(
                        "name" to producto.name,
                        "quantity" to producto.quantity,
                        "unitPrice" to producto.unitPrice
                    )
                    docRef.collection("productos").add(productoMap)
                }
            }
    }

    // Limpia la lista en memoria (no afecta Firestore)
    fun clear() {
        _shoppingLists.value = mutableListOf()
    }

    // Método opcional para añadir listas de múltiples supermercados al mismo tiempo
    fun addSupermarketLists(mercadona: List<Producto>, carrefour: List<Producto>) {
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val now = sdf.format(Date())

        val listaMercadona = ShoppingList(
            dateTime = now,
            storeName = "Mercadona",
            products = mercadona,
            isExpanded = false,
            total = mercadona.sumOf { it.unitPrice * it.quantity }
        )

        val listaCarrefour = ShoppingList(
            dateTime = now,
            storeName = "Carrefour",
            products = carrefour,
            isExpanded = false,
            total = carrefour.sumOf { it.unitPrice * it.quantity }
        )

        _shoppingLists.value?.addAll(listOf(listaMercadona, listaCarrefour))
        _shoppingLists.postValue(_shoppingLists.value)
    }
}
