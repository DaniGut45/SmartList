package com.example.smartlist.view

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.smartlist.R
import com.example.smartlist.model.Producto
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.widget.ArrayAdapter
import android.text.Editable
import android.text.TextWatcher
import com.example.smartlist.model.ProductoConPrecio


class CreateListFragment : Fragment() {

    private lateinit var etProduct: EditText
    private lateinit var etQuantity: EditText
    private lateinit var mercadonaContainer: LinearLayout
    private lateinit var carrefourContainer: LinearLayout

    private val productList = mutableListOf<ProductoConPrecio>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_crear_lista, container, false)

        etProduct = view.findViewById(R.id.et_product)
        etQuantity = view.findViewById(R.id.et_quantity)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_product)
        mercadonaContainer = view.findViewById(R.id.mercadona_container)
        carrefourContainer = view.findViewById(R.id.carrefour_container)

        btnAdd.setOnClickListener {
            val nombre = etProduct.text.toString().trim().replaceFirstChar { it.uppercase() }
            val cantidad = etQuantity.text.toString().toIntOrNull() ?: 1

            if (nombre.isNotEmpty()) {
                val db = Firebase.firestore
                db.collection("productos")
                    .whereEqualTo("nombre", nombre)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val doc = documents.first()
                            val idProducto = doc.id

                            val precios = mutableMapOf<String, Double>()
                            val supermercados = listOf("mercadona", "carrefour")

                            var fetched = 0
                            for (s in supermercados) {
                                db.collection("productos")
                                    .document(idProducto)
                                    .collection("precios")
                                    .document(s)
                                    .get()
                                    .addOnSuccessListener { precioDoc ->
                                        val precio = precioDoc.getDouble("precio")
                                        if (precio != null) {
                                            precios[s] = precio
                                        }
                                        fetched++
                                        if (fetched == supermercados.size) {
                                            if (precios.size == supermercados.size) {
                                                productList.add(
                                                    ProductoConPrecio(
                                                        nombre,
                                                        cantidad,
                                                        precios["mercadona"] ?: 0.0,
                                                        precios["carrefour"] ?: 0.0
                                                    )
                                                )

                                                updateSupermarketViews()
                                                etProduct.text.clear()
                                                etQuantity.text.clear()

                                                val imm = requireContext().getSystemService(
                                                    android.content.Context.INPUT_METHOD_SERVICE
                                                ) as android.view.inputmethod.InputMethodManager
                                                imm.hideSoftInputFromWindow(view?.windowToken, 0)
                                            }
                                        }
                                    }
                            }
                        } else {
                            Toast.makeText(requireContext(), "Producto no encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        etProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().trim().replaceFirstChar { it.uppercase() }

                if (input.length >= 1) {
                    Firebase.firestore.collection("productos")
                        .orderBy("nombre")
                        .startAt(input)
                        .endAt(input + "\uf8ff")
                        .get()
                        .addOnSuccessListener { result ->
                            val sugerencias = result.mapNotNull { it.getString("nombre") }
                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sugerencias)
                            (etProduct as AutoCompleteTextView).setAdapter(adapter)
                            (etProduct as AutoCompleteTextView).showDropDown()
                        }
                }
            }
        })

        return view
    }

    private fun updateSupermarketViews() {
        updateSupermarketView("Mercadona", mercadonaContainer)
        updateSupermarketView("Carrefour", carrefourContainer)
    }

    private fun updateSupermarketView(name: String, container: LinearLayout) {
        container.removeAllViews()

        val header = layoutInflater.inflate(R.layout.supermarket_header, container, false)
        header.findViewById<TextView>(R.id.tv_super_title).text = name
        header.findViewById<ImageView>(R.id.iv_logo).setImageResource(
            if (name == "Mercadona") R.drawable.logo_mercadona else R.drawable.logo_carrefour
        )
        container.addView(header)

        var total = 0.0

        for (producto in productList) {
            val unitPrice = if (name == "Mercadona") producto.precioMercadona else producto.precioCarrefour
            val totalPrice = unitPrice * producto.cantidad

            val itemView = layoutInflater.inflate(R.layout.item_producto, container, false)
            val nombreProducto = itemView.findViewById<TextView>(R.id.tv_nombre_producto)
            val precioProducto = itemView.findViewById<TextView>(R.id.tv_precio_producto)
            val btnBorrar = itemView.findViewById<Button>(R.id.btn_borrar)

            nombreProducto.text = producto.nombre
            precioProducto.text = "Precio: %.2f€".format(totalPrice)

            btnBorrar.setOnClickListener {
                productList.remove(producto)
                updateSupermarketViews()
            }

            total += totalPrice
            container.addView(itemView)
        }

        val totalText = TextView(requireContext()).apply {
            text = "Total: %.2f€".format(total)
            setTextColor(resources.getColor(R.color.smoky_black, null))
            setPadding(0, 16, 0, 0)
        }

        val btnAddList = Button(requireContext()).apply {
            text = "Añadir Lista"
            setBackgroundResource(R.drawable.rounded_button_bg)
            setTextColor(resources.getColor(R.color.smoky_black, null))
            setPadding(0, 16, 0, 32)

            setOnClickListener {
                val viewModel = (activity as MainActivity).shoppingListViewModel

                val productos = productList.map {
                    Producto(
                        name = it.nombre,
                        quantity = it.cantidad,
                        unitPrice = if (name == "Mercadona") it.precioMercadona else it.precioCarrefour
                    )
                }

                viewModel.addList(
                    supermarket = name,
                    productos = productos
                )

                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(R.id.fragmentContainer, MainFragment())
                    .commit()

                (activity as? MainActivity)?.updateBottomNavColors("home")
            }
        }

        container.addView(totalText)
        container.addView(btnAddList)
    }
}
