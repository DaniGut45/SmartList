package com.example.smartlist.model

// Variante de Producto que incluye precios diferenciados por supermercado.
// Útil para comparar y calcular diferencias o elegir el supermercado más económico.
data class ProductoConPrecio(
    val nombre: String,
    val cantidad: Int,
    val precioMercadona: Double,
    val precioCarrefour: Double,
    val precioDia: Double
)
