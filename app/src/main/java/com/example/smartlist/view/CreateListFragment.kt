package com.example.smartlist.view

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.smartlist.R

class CreateListFragment : Fragment() {

    private lateinit var etProduct: EditText
    private lateinit var etQuantity: EditText
    private lateinit var mercadonaContainer: LinearLayout
    private lateinit var carrefourContainer: LinearLayout

    // Lista única de productos con su cantidad
    private val productList = mutableListOf<Pair<String, Int>>()

    // Precios simulados por producto
    private val prices = mapOf(
        "Tomates" to 1.30,
        "Leche" to 1.10,
        "Pan" to 1.40
    )

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
            val product = etProduct.text.toString().trim().replaceFirstChar { it.uppercase() }
            val quantity = etQuantity.text.toString().toIntOrNull() ?: 1

            if (product.isNotEmpty()) {
                productList.add(product to quantity)
                updateSupermarketViews()

                // ⬇️ OCULTAR TECLADO
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)

                etProduct.text.clear()
                etQuantity.text.clear()
            }
        }


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
        val productosConPrecio = mutableListOf<Pair<String, Int>>() // para luego guardar

        for ((product, quantity) in productList) {
            val unitPrice = (0..500).random() / 100.0 // precio aleatorio entre 0.00 y 5.00 €
            val totalPrice = unitPrice * quantity

            productosConPrecio.add(product to quantity) // seguimos usando productList como base

            val itemView = layoutInflater.inflate(R.layout.item_producto, container, false)
            val nombreProducto = itemView.findViewById<TextView>(R.id.tv_nombre_producto)
            val precioProducto = itemView.findViewById<TextView>(R.id.tv_precio_producto)
            val btnBorrar = itemView.findViewById<Button>(R.id.btn_borrar)

            nombreProducto.text = product
            precioProducto.text = "Precio: %.2f€".format(totalPrice)

            btnBorrar.setOnClickListener {
                productList.remove(product to quantity)
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

                // Generamos los productos con precio aleatorio para este súper
                val productos = productList.map {
                    com.example.smartlist.model.Producto(
                        name = it.first,
                        quantity = it.second,
                        unitPrice = (0..500).random() / 100.0
                    )
                }

                viewModel.addList(
                    supermarket = name,
                    products = productos.map { it.name to it.quantity }
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
