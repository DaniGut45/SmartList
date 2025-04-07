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

    fun addList(supermarket: String, products: List<Pair<String, Int>>) {
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val now = sdf.format(Date())

        val productList = products.map {
            Producto(
                name = it.first,
                quantity = it.second,
                unitPrice = (0..500).random() / 100.0
            )
        }

        val newList = ShoppingList(
            dateTime = now,
            storeName = supermarket,
            products = productList
        )

        _shoppingLists.value?.add(newList)
        _shoppingLists.postValue(_shoppingLists.value)
    }

    fun clear() {
        _shoppingLists.value = mutableListOf()
    }

    fun addSupermarketLists(products: List<Pair<String, Int>>) {
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val now = sdf.format(Date())

        val mercadonaList = products.map {
            Producto( // en lugar de Product
                name = it.first,
                quantity = it.second,
                unitPrice = (0..500).random() / 100.0
            )
        }


        val carrefourList = products.map {
            Producto(
                name = it.first,
                quantity = it.second,
                unitPrice = (0..500).random() / 100.0
            )
        }

        val mercadona = ShoppingList(
            dateTime = now,
            storeName = "Mercadona",
            products = mercadonaList
        )

        val carrefour = ShoppingList(
            dateTime = now,
            storeName = "Carrefour",
            products = carrefourList
        )

        _shoppingLists.value?.addAll(listOf(mercadona, carrefour))
        _shoppingLists.postValue(_shoppingLists.value)
    }

}
