package com.example.smartlist.model

// Representa una lista de compras con información asociada como fecha, tienda y productos incluidos.
// Se usa probablemente en un historial o vista expandible (por ejemplo, en un RecyclerView).
data class ShoppingList(
    var id: String = "", // <-- Este campo es clave
    val dateTime: String,            // Fecha y hora de la compra (formato como ISO 8601 recomendado)
    val storeName: String,           // Nombre de la tienda donde se realizó la compra
    val products: List<Producto>,    // Lista de productos comprados
    var isExpanded: Boolean = false, // Estado de expansión en la interfaz (usado en UI para mostrar/ocultar detalles)
    val total: Double                // Total de la compra (ya calculado, para evitar recalcular cada vez)
)
