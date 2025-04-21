package com.example.smartlist.view

import android.annotation.SuppressLint
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
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Typeface
import com.example.smartlist.model.ProductoConPrecio

class CreateListFragment : Fragment() {

    private lateinit var etProduct: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var mercadonaContainer: LinearLayout
    private lateinit var carrefourContainer: LinearLayout
    private lateinit var alcampoContainer: LinearLayout
    private val productList = mutableListOf<ProductoConPrecio>()
    private var shouldSkipAutoComplete = false


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_crear_lista, container, false)

        etProduct = view.findViewById(R.id.et_product)
        etQuantity = view.findViewById(R.id.et_quantity)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_product)
        mercadonaContainer = view.findViewById(R.id.mercadona_container)
        carrefourContainer = view.findViewById(R.id.carrefour_container)
        alcampoContainer = view.findViewById(R.id.alcampo_container)

        val loader = view.findViewById<ProgressBar>(R.id.progress_loader)



        val displayMetrics = resources.displayMetrics
        etProduct.post {
            etProduct.dropDownWidth = (displayMetrics.widthPixels * 0.60).toInt()  // 75% de la pantalla
            etProduct.dropDownHorizontalOffset = (displayMetrics.widthPixels * 0.0025).toInt()
        }

        // AutoComplete click listener para ocultar todo
        etProduct.setOnItemClickListener { parent, view, position, id ->
            val selected = parent.getItemAtPosition(position).toString()
            shouldSkipAutoComplete = true
            etProduct.setText(selected)
            etProduct.dismissDropDown()
            etProduct.clearFocus()

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etProduct.windowToken, 0)
        }

        etProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (shouldSkipAutoComplete) {
                    shouldSkipAutoComplete = false
                    return
                }

                val input = s.toString().trim().replaceFirstChar { it.uppercase() }
                if (input.length >= 1) {
                    // Mostrar loader
                    val loader = view?.findViewById<ProgressBar>(R.id.progress_loader)
                    loader?.visibility = View.VISIBLE

                    Firebase.firestore.collection("productos")
                        .orderBy("nombre")
                        .startAt(input)
                        .endAt(input + "\uf8ff")
                        .get()
                        .addOnSuccessListener { result ->
                            val sugerencias = result.mapNotNull { it.getString("nombre") }

                            val adapter = object : ArrayAdapter<String>(
                                requireContext(),
                                R.layout.item_dropdown,
                                sugerencias
                            ) {
                                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_dropdown, parent, false)
                                    val textView = view.findViewById<TextView>(R.id.dropdown_text)
                                    textView.text = getItem(position)
                                    return view
                                }
                            }

                            etProduct.setAdapter(adapter)
                            etProduct.showDropDown()
                            loader?.visibility = View.GONE // Ocultar loader al terminar
                        }
                        .addOnFailureListener {
                            loader?.visibility = View.GONE // Ocultar loader también si hay error
                            Toast.makeText(requireContext(), "Error al buscar", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        })

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
                            db.collection("productos")
                                .document(idProducto)
                                .get()
                                .addOnSuccessListener { doc ->
                                    val supermercadoMap = doc.get("supermercado") as? Map<String, Map<String, Double>>
                                    if (supermercadoMap != null) {
                                        val precioMercadona = supermercadoMap["mercadona"]?.get("precio") ?: 0.0
                                        val precioCarrefour = supermercadoMap["carrefour"]?.get("precio") ?: 0.0
                                        val precioAlcampo = supermercadoMap["alcampo"]?.get("precio") ?: 0.0

                                        productList.add(
                                            ProductoConPrecio(
                                                nombre,
                                                cantidad,
                                                precioMercadona,
                                                precioCarrefour,
                                                precioAlcampo
                                            )
                                        )
                                        updateSupermarketViews()
                                        etProduct.text.clear()
                                        etQuantity.text.clear()

                                        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                        imm.hideSoftInputFromWindow(view?.windowToken, 0)
                                    }
                                }

                        } else {
                            Toast.makeText(requireContext(), "Producto no encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        return view
    }

    private fun updateSupermarketViews() {
        updateSupermarketView("Mercadona", mercadonaContainer)
        updateSupermarketView("Carrefour", carrefourContainer)
        updateSupermarketView("Alcampo", alcampoContainer)
    }

    private fun updateSupermarketView(name: String, container: LinearLayout) {
        container.removeAllViews()

        val header = layoutInflater.inflate(R.layout.supermarket_header, container, false)
        header.findViewById<TextView>(R.id.tv_super_title).text = name
        val logo = when (name.lowercase()) {
            "mercadona" -> R.drawable.logo_mercadona
            "carrefour" -> R.drawable.logo_carrefour
            "alcampo" -> R.drawable.logo_alcampo
            else -> R.drawable.logo_app // Por si acaso
        }
        header.findViewById<ImageView>(R.id.iv_logo).setImageResource(logo)

        val container = when (name.lowercase()) {
            "mercadona" -> view?.findViewById<LinearLayout>(R.id.mercadona_container)
            "carrefour" -> view?.findViewById<LinearLayout>(R.id.carrefour_container)
            "alcampo" -> view?.findViewById<LinearLayout>(R.id.alcampo_container)
            else -> null
        }

        container?.addView(header)

        var total = 0.0
        for (producto in productList) {
            val unitPrice = when (name) {
                "Mercadona" -> producto.precioMercadona
                "Carrefour" -> producto.precioCarrefour
                "Alcampo" -> producto.precioAlcampo
                else -> 0.0
            }

            // Calcular precio mínimo entre supermercados para este producto
            val minPrice = listOf(
                producto.precioMercadona,
                producto.precioCarrefour,
                producto.precioAlcampo
            ).minOrNull() ?: 0.0

            val totalPrice = unitPrice * producto.cantidad

            val itemView = layoutInflater.inflate(R.layout.item_producto, container, false)
            val tvNombre = itemView.findViewById<TextView>(R.id.tv_nombre_producto)
            val tvPrecio = itemView.findViewById<TextView>(R.id.tv_precio_producto)
            val btnBorrar = itemView.findViewById<Button>(R.id.btn_borrar)

            // Texto del producto, con estrella si es el más barato
            tvNombre.text = if (unitPrice == minPrice) "${producto.nombre} ⭐" else producto.nombre

            // Estilo en negrita si es el más barato
            tvNombre.setTypeface(null, if (unitPrice == minPrice) Typeface.BOLD else Typeface.NORMAL)

            // Precio
            tvPrecio.text = "Precio: %.2f€".format(totalPrice)

            btnBorrar.setOnClickListener {
                productList.remove(producto)
                updateSupermarketViews()
            }

            total += totalPrice
            container?.addView(itemView)
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
                        unitPrice = when (name) {
                            "Mercadona" -> it.precioMercadona
                            "Carrefour" -> it.precioCarrefour
                            "Alcampo" -> it.precioAlcampo
                            else -> 0.0
                        }
                    )
                }

                viewModel.addList(supermarket = name, productos = productos)

                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, MainFragment())
                    .commit()

                (activity as? MainActivity)?.updateBottomNavColors("home")
            }
        }

        if (container != null) {
            container.addView(totalText)
            container.addView(btnAddList)
        }

    }
}
