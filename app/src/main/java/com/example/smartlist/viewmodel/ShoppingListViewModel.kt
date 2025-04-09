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

    // Este método ahora recibe productos con precios reales
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

        _shoppingLists.value?.add(list)
        _shoppingLists.postValue(_shoppingLists.value)

        // Guardar en Firestore si está logueado
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
                // Añadir productos a subcolección
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

    fun clear() {
        _shoppingLists.value = mutableListOf()
    }

    // Si todavía quieres añadir Mercadona y Carrefour a la vez, puedes mantener este método (opcional)
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
