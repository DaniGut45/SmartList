package com.example.smartlist.viewmodel

// Importaciones necesarias
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

// ViewModel encargado de manejar las listas de la compra de forma reactiva y centralizada
class ShoppingListViewModel : ViewModel() {

    // Lista de compras en memoria (Mutable para modificar internamente)
    private val _shoppingLists = MutableLiveData<MutableList<ShoppingList>>(mutableListOf())

    // Versión pública e inmutable para observar las listas desde los fragments
    val shoppingLists: LiveData<MutableList<ShoppingList>> = _shoppingLists

    // Función que añade una nueva lista de la compra
    fun addList(supermarket: String, productos: List<Producto>) {
        // Formateamos la fecha y hora actuales
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val now = sdf.format(Date())

        // Calculamos el total de la lista sumando (precio * cantidad) de todos los productos
        val total = productos.sumOf { it.unitPrice * it.quantity }

        // Creamos el objeto ShoppingList
        val list = ShoppingList(
            dateTime = now,
            storeName = supermarket,
            products = productos,
            total = total
        )

        // Añadimos la nueva lista en memoria
        _shoppingLists.value?.add(list)
        _shoppingLists.postValue(_shoppingLists.value) // Notificamos a los observers

        // Guardamos la lista también en Firestore si el usuario está logueado
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore

        // Datos básicos de la lista (sin productos aún)
        val listData = mapOf(
            "dateTime" to now,
            "storeName" to supermarket,
            "total" to total
        )

        // Añadimos la lista al usuario en Firestore
        db.collection("usuarios").document(userId)
            .collection("listas").add(listData)
            .addOnSuccessListener { docRef ->
                // Una vez añadida, subimos cada producto como subdocumento
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

    // Limpia las listas en memoria (no borra en Firestore)
    fun clear() {
        _shoppingLists.value = mutableListOf()
    }

    // Añade listas de varios supermercados al mismo tiempo (Mercadona y Dia)
    fun addSupermarketLists(mercadona: List<Producto>, dia: List<Producto>) {
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val now = sdf.format(Date())

        // Creamos lista de Mercadona
        val listaMercadona = ShoppingList(
            dateTime = now,
            storeName = "Mercadona",
            products = mercadona,
            isExpanded = false,
            total = mercadona.sumOf { it.unitPrice * it.quantity }
        )

        // Creamos lista de Dia
        val listaDia = ShoppingList(
            dateTime = now,
            storeName = "Dia",
            products = dia,
            isExpanded = false,
            total = dia.sumOf { it.unitPrice * it.quantity }
        )

        // Añadimos ambas listas a la memoria local
        _shoppingLists.value?.addAll(listOf(listaMercadona, listaDia))
        _shoppingLists.postValue(_shoppingLists.value) // Notificamos a los observers
    }

    // Carga todas las listas del usuario desde Firestore
    fun loadListsFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore

        db.collection("usuarios")
            .document(userId)
            .collection("listas")
            .get()
            .addOnSuccessListener { listasDocs ->
                val listasTemp = mutableListOf<ShoppingList>()

                listasDocs.forEach { doc ->
                    val dateTime = doc.getString("dateTime") ?: ""
                    val store = doc.getString("storeName") ?: ""
                    val total = doc.getDouble("total") ?: 0.0

                    // Cargamos los productos asociados a esta lista
                    doc.reference.collection("productos").get()
                        .addOnSuccessListener { productosDocs ->
                            val productos = productosDocs.map {
                                Producto(
                                    it.getString("name") ?: "",
                                    (it.getLong("quantity") ?: 0).toInt(),
                                    it.getDouble("unitPrice") ?: 0.0
                                )
                            }

                            // Creamos el objeto ShoppingList completo
                            val lista = ShoppingList(dateTime, store, productos, false, total)
                            listasTemp.add(lista)

                            // Cuando terminemos de cargar todos los productos de todas las listas
                            if (listasTemp.size == listasDocs.size()) {
                                _shoppingLists.value = listasTemp // Actualizamos el LiveData
                            }
                        }
                }
            }
    }
}
