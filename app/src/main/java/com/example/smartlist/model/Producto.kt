package com.example.smartlist.model

// Clase de datos que representa un producto dentro de una lista.
// Al ser una data class, se generan automáticamente métodos útiles como equals(), hashCode() y toString(),
// lo cual facilita el manejo de objetos en listas, comparaciones o almacenamiento.
data class Producto(
    val name: String,
    val quantity: Int,
    val unitPrice: Double
)
