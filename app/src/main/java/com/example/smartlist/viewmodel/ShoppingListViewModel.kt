package com.example.smartlist.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartlist.model.Producto
import com.example.smartlist.model.ShoppingList
import java.text.SimpleDateFormat
import java.util.*

class ShoppingListViewModel : ViewModel() {

    private val _shoppingLists = MutableLiveData<MutableList<ShoppingList>>(mutableListOf())
    val shoppingLists: LiveData<MutableList<ShoppingList>> = _shoppingLists

    // Este método ahora recibe productos con precios reales
    fun addList(supermarket: String, productos: List<Producto>) {
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val now = sdf.format(Date())

        val totalLista = productos.sumOf { it.unitPrice * it.quantity }

        val nuevaLista = ShoppingList(
            dateTime = now,
            storeName = supermarket,
            products = productos,
            isExpanded = false,
            total = totalLista
        )

        _shoppingLists.value?.add(nuevaLista)
        _shoppingLists.postValue(_shoppingLists.value)
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
