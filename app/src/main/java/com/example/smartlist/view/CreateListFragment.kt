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
import java.text.Normalizer

class CreateListFragment : Fragment() {

    private lateinit var etProduct: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var mercadonaContainer: LinearLayout
    private lateinit var diaContainer: LinearLayout
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
        diaContainer = view.findViewById(R.id.dia_container)

        val loader = view.findViewById<ProgressBar>(R.id.progress_loader)

        val displayMetrics = resources.displayMetrics
        etProduct.post {
            etProduct.dropDownWidth = (displayMetrics.widthPixels * 0.60).toInt()
            etProduct.dropDownHorizontalOffset = (displayMetrics.widthPixels * 0.0025).toInt()
        }

        etProduct.setOnItemClickListener { parent, _, position, _ ->
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

                val input = s.toString().trim()
                val palabras = normalizar(input).split(" ").filter { it.isNotBlank() }

                if (palabras.isNotEmpty()) {
                    loader.visibility = View.VISIBLE

                    Firebase.firestore.collection("productos")
                        .whereArrayContains("keywords", palabras[0])
                        .get()
                        .addOnSuccessListener { result ->
                            val filtrados = result.filter { doc ->
                                val keywords = doc.get("keywords") as? List<*> ?: emptyList<String>()
                                palabras.all { palabra -> keywords.contains(palabra) }
                            }

                            val sugerencias = filtrados.mapNotNull { it.getString("nombre") }

                            val adapter = object : ArrayAdapter<String>(
                                requireContext(), R.layout.item_dropdown, sugerencias
                            ) {
                                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                    val view = convertView ?: LayoutInflater.from(context)
                                        .inflate(R.layout.item_dropdown, parent, false)
                                    val textView = view.findViewById<TextView>(R.id.dropdown_text)
                                    textView.text = getItem(position)
                                    return view
                                }
                            }

                            etProduct.setAdapter(adapter)
                            etProduct.showDropDown()
                            loader.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            loader.visibility = View.GONE
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
                            db.collection("productos").document(idProducto).get()
                                .addOnSuccessListener { doc ->
                                    val supermercadoMap = doc.get("supermercado") as? Map<*, *>
                                    if (supermercadoMap != null) {
                                        val precioMercadona = (supermercadoMap["mercadona"] as? Map<*, *>)?.get("precio") as? Number
                                        val precioDia = (supermercadoMap["dia"] as? Map<*, *>)?.get("precio") as? Number

                                        val finalPrecioMercadona = precioMercadona?.toDouble() ?: -1.0
                                        val finalPrecioDia = precioDia?.toDouble() ?: -1.0

                                        productList.add(
                                            ProductoConPrecio(nombre, cantidad, finalPrecioMercadona, finalPrecioDia)
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
        updateSupermarketView("Dia", diaContainer)
    }

    private fun normalizar(texto: String): String {
        val normalized = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return normalized.replace("\\p{Mn}+".toRegex(), "").lowercase()
    }

    private fun updateSupermarketView(name: String, container: LinearLayout) {
        container.removeAllViews()

        val header = layoutInflater.inflate(R.layout.supermarket_header, container, false)
        header.findViewById<TextView>(R.id.tv_super_title).text = name
        val logo = when (name.lowercase()) {
            "mercadona" -> R.drawable.logo_mercadona
            "dia" -> R.drawable.logo_dia
            else -> R.drawable.logo_app
        }
        header.findViewById<ImageView>(R.id.iv_logo).setImageResource(logo)

        val containerReal = when (name.lowercase()) {
            "mercadona" -> view?.findViewById<LinearLayout>(R.id.mercadona_container)
            "dia" -> view?.findViewById<LinearLayout>(R.id.dia_container)
            else -> null
        }

        containerReal?.addView(header)

        var total = 0.0
        var totalNoDisponibles = 0.0

        for (producto in productList) {
            val mercadonaDisponible = producto.precioMercadona != -1.0
            val diaDisponible = producto.precioDia != -1.0
            val disponibleEnAmbos = mercadonaDisponible && diaDisponible

            val unitPrice = when (name) {
                "Mercadona" -> producto.precioMercadona
                "Dia" -> producto.precioDia
                else -> 0.0
            }

            val cantidad = producto.cantidad
            val totalPrice = unitPrice * cantidad

            val itemView = layoutInflater.inflate(R.layout.item_producto, container, false)
            val tvNombre = itemView.findViewById<TextView>(R.id.tv_nombre_producto)
            val tvPrecio = itemView.findViewById<TextView>(R.id.tv_precio_producto)
            val btnBorrar = itemView.findViewById<Button>(R.id.btn_borrar)

            tvNombre.text = producto.nombre

            if (unitPrice == -1.0) {
                tvPrecio.text = "No disponible"
                tvPrecio.setTextColor(resources.getColor(R.color.red, null))
                totalNoDisponibles += when (name) {
                    "Mercadona" -> if (mercadonaDisponible) producto.precioMercadona * cantidad else 0.0
                    "Dia" -> if (diaDisponible) producto.precioDia * cantidad else 0.0
                    else -> 0.0
                }
            } else {
                tvPrecio.text = "Precio: %.2f€ (x%d)".format(totalPrice, cantidad)
                tvPrecio.setTextColor(resources.getColor(R.color.smoky_black, null))

                if (mercadonaDisponible && diaDisponible) {
                    total += totalPrice
                }
            }

            btnBorrar.setOnClickListener {
                productList.remove(producto)
                updateSupermarketViews()
            }

            containerReal?.addView(itemView)
        }

        val totalText = TextView(requireContext()).apply {
            text = if (totalNoDisponibles > 0.0) {
                "Total: %.2f€ · Incluye %.2f€ en productos no disponibles".format(total, totalNoDisponibles)
            } else {
                "Total: %.2f€".format(total)
            }
            setTextColor(resources.getColor(R.color.smoky_black, null))
            setPadding(0, 16, 0, 0)
            textSize = 16f
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
                            "Dia" -> it.precioDia
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

        containerReal?.addView(totalText)
        containerReal?.addView(btnAddList)
    }
}
