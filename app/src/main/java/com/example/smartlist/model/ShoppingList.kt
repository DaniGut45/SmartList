package com.example.smartlist.model

data class ShoppingList(
    val dateTime: String,
    val storeName: String,
    val products: List<Producto>,
    var isExpanded: Boolean = false,
    val total: Double
)
