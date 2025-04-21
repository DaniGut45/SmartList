package com.example.smartlist.model

data class ProductoConPrecio(
    val nombre: String,
    val cantidad: Int,
    val precioMercadona: Double,
    val precioCarrefour: Double,
    val precioAlcampo: Double
)
