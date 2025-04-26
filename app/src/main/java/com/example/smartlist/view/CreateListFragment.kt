package com.example.smartlist.view

// Importaciones necesarias
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
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.animation.AnimationUtils
import com.example.smartlist.utils.SessionManager
import com.example.smartlist.model.ProductoConPrecio
import java.text.Normalizer
import androidx.appcompat.app.AlertDialog

// Fragmento donde se crea una nueva lista de productos
class CreateListFragment : Fragment() {

    // Declaraciones de vistas
    private lateinit var etProduct: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var mercadonaContainer: LinearLayout
    private lateinit var diaContainer: LinearLayout

    // Lista de productos añadidos en esta sesión
    private val productList = mutableListOf<ProductoConPrecio>()

    // Flag para evitar re-activar el autocompletado cuando seleccionamos un producto
    private var shouldSkipAutoComplete = false

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_crear_lista, container, false)

        // Inicialización de vistas
        etProduct = view.findViewById(R.id.et_product)
        etQuantity = view.findViewById(R.id.et_quantity)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_product)
        mercadonaContainer = view.findViewById(R.id.mercadona_container)
        diaContainer = view.findViewById(R.id.dia_container)
        val loader = view.findViewById<ProgressBar>(R.id.progress_loader)

        // Ajustes de tamaño del desplegable de sugerencias
        val displayMetrics = resources.displayMetrics
        etProduct.post {
            etProduct.dropDownWidth = (displayMetrics.widthPixels * 0.60).toInt()
            etProduct.dropDownHorizontalOffset = (displayMetrics.widthPixels * 0.0025).toInt()
        }

        // Cuando seleccionamos un producto del autocompletado
        etProduct.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            shouldSkipAutoComplete = true
            etProduct.setText(selected)
            etProduct.dismissDropDown()
            etProduct.clearFocus()

            // Cierra el teclado
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etProduct.windowToken, 0)
        }

        // Observador de cambios de texto para buscar productos automáticamente
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

                    // Buscamos coincidencias en Firebase
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

        // Botón para añadir un producto a la lista
        btnAdd.setOnClickListener {
            val nombre = etProduct.text.toString().trim().replaceFirstChar { it.uppercase() }
            val cantidadInput = etQuantity.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, introduce un producto.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cantidad = cantidadInput.toIntOrNull() ?: run {
                Toast.makeText(requireContext(), "Cantidad no especificada o inválida. Se asigna 1.", Toast.LENGTH_SHORT).show()
                1
            }

            // Consultamos si existe en la base de datos
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

                                    // Oculta teclado tras añadir producto
                                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), "Producto no encontrado en la base de datos.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        return view
    }

    // 🛒 Actualiza visualmente las vistas de supermercados
    private fun updateSupermarketViews() {
        var totalMercadona = 0.0
        var totalDia = 0.0
        var noDisponiblesMercadona = 0
        var noDisponiblesDia = 0

        for (producto in productList) {
            val mercadonaDisponible = producto.precioMercadona != -1.0
            val diaDisponible = producto.precioDia != -1.0

            if (mercadonaDisponible && diaDisponible) {
                totalMercadona += producto.precioMercadona * producto.cantidad
                totalDia += producto.precioDia * producto.cantidad
            }

            if (!mercadonaDisponible) noDisponiblesMercadona++
            if (!diaDisponible) noDisponiblesDia++
        }

        val supermercadoMasBarato = when {
            totalMercadona < totalDia -> "Mercadona"
            totalDia < totalMercadona -> "Dia"
            else -> "Igual"
        }

        // Actualiza cada supermercado
        updateSupermarketView("Mercadona", mercadonaContainer, supermercadoMasBarato == "Mercadona", noDisponiblesMercadona)
        updateSupermarketView("Dia", diaContainer, supermercadoMasBarato == "Dia", noDisponiblesDia)
    }

    // 🔢 Calcula el total de una lista para un supermercado específico
    private fun calculateTotal(supermarket: String): Double {
        var total = 0.0
        for (producto in productList) {
            val unitPrice = when (supermarket) {
                "Mercadona" -> producto.precioMercadona
                "Dia" -> producto.precioDia
                else -> 0.0
            }
            if (unitPrice != -1.0) {
                total += unitPrice * producto.cantidad
            }
        }
        return total
    }

    // 🔠 Normaliza textos (quita acentos, pasa a minúsculas)
    private fun normalizar(texto: String): String {
        val normalized = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return normalized.replace("\\p{Mn}+".toRegex(), "").lowercase()
    }

    // 📦 Actualiza visualmente un supermercado individual (Mercadona o Dia)
    private fun updateSupermarketView(name: String, container: LinearLayout, esMasBarato: Boolean, noDisponibles: Int) {
        container.removeAllViews()

        val backgroundResource = if (esMasBarato) {
            R.drawable.green_border_background
        } else {
            R.drawable.default_background
        }
        container.setBackgroundResource(backgroundResource)

        val header = layoutInflater.inflate(R.layout.supermarket_header, container, false)
        header.findViewById<TextView>(R.id.tv_super_title).text = name

        // Seteamos el logo
        val logo = when (name.lowercase()) {
            "mercadona" -> R.drawable.logo_mercadona
            "dia" -> R.drawable.logo_dia
            else -> R.drawable.logo_app
        }
        header.findViewById<ImageView>(R.id.iv_logo).setImageResource(logo)

        // Mostramos el contador de productos no disponibles
        val badgeNoDisponible = header.findViewById<TextView>(R.id.badge_no_disponibles)
        if (noDisponibles > 0) {
            badgeNoDisponible.visibility = View.VISIBLE
            badgeNoDisponible.text = noDisponibles.toString()
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.popup_in)
            badgeNoDisponible.startAnimation(animation)
        } else {
            badgeNoDisponible.visibility = View.GONE
        }

        val containerReal = when (name.lowercase()) {
            "mercadona" -> view?.findViewById<LinearLayout>(R.id.mercadona_container)
            "dia" -> view?.findViewById<LinearLayout>(R.id.dia_container)
            else -> null
        }

        containerReal?.addView(header)

        var total = 0.0
        var totalBruto = 0.0

        for (producto in productList) {
            val mercadonaDisponible = producto.precioMercadona != -1.0
            val diaDisponible = producto.precioDia != -1.0
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
            } else {
                // 🧠 Ahora detectamos si este producto es el más barato
                val esMercadona = name == "Mercadona"
                val precioEsteSuper = if (esMercadona) producto.precioMercadona else producto.precioDia
                val precioOtroSuper = if (esMercadona) producto.precioDia else producto.precioMercadona

                val totalPrice = precioEsteSuper * cantidad

                if (precioEsteSuper != -1.0 && precioOtroSuper != -1.0 && precioEsteSuper < precioOtroSuper) {
                    // ⭐ Producto más barato → ponemos negrita y estrella
                    tvPrecio.text = "⭐ Precio: %.2f€ (x%d)".format(totalPrice, cantidad)
                    tvPrecio.setTypeface(null, Typeface.BOLD)
                } else {
                    // Producto normal
                    tvPrecio.text = "Precio: %.2f€ (x%d)".format(totalPrice, cantidad)
                    tvPrecio.setTypeface(null, Typeface.NORMAL)
                }

                tvPrecio.setTextColor(resources.getColor(R.color.smoky_black, null))
            }

            btnBorrar.setOnClickListener {
                productList.remove(producto)
                updateSupermarketViews()
            }

            containerReal?.addView(itemView)
        }

        // Texto del total
        val totalText = TextView(requireContext()).apply {
            if (totalBruto > 0.0 && total != totalBruto) {
                val mainText = "Total: %.2f€ ".format(total)
                val extraText = "(Valor bruto: %.2f€)".format(totalBruto)
                val fullText = mainText + extraText
                val spannable = SpannableString(fullText)
                spannable.setSpan(RelativeSizeSpan(0.8f), mainText.length, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = spannable
            } else {
                text = "Total: %.2f€".format(total)
            }
            setTextColor(resources.getColor(R.color.smoky_black, null))
            setPadding(0, 16, 0, 0)
            textSize = 16f
        }

        // Botón para guardar la lista
        val btnAddList = Button(requireContext()).apply {
            text = "Añadir Lista"
            setBackgroundResource(R.drawable.rounded_button_bg)
            setTextColor(resources.getColor(R.color.smoky_black, null))
            setPadding(0, 16, 0, 32)

            setOnClickListener {
                if (!SessionManager.isLoggedIn) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Iniciar sesión requerida")
                        .setPositiveButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                        .setMessage("Debes iniciar sesión para guardar tu lista. ¿Deseas iniciar sesión ahora?")
                        .setNegativeButton("Iniciar sesión") { dialog, _ ->
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                                .replace(R.id.fragmentContainer, LoginFragment())
                                .commit()
                            (activity as? MainActivity)?.updateBottomNavColors("profile")
                            dialog.dismiss()
                        }
                        .show()
                } else {
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
        }

        containerReal?.addView(totalText)
        containerReal?.addView(btnAddList)
    }
}
