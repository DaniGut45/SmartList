package com.example.smartlist.view.adapters

// Importaciones necesarias
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlist.R
import com.example.smartlist.model.ShoppingList

// Adaptador del RecyclerView que muestra todas las listas de compra
class ListAdapter(
    private val list: MutableList<ShoppingList> // Lista de datos que el adaptador manejará
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    // ViewHolder interno que representa cada ítem del RecyclerView
    inner class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Referencias a las vistas del layout de un solo ítem
        val dateTime: TextView = view.findViewById(R.id.tv_date_time)   // Fecha y hora de la compra
        val store: TextView = view.findViewById(R.id.tv_store)           // Nombre del supermercado
        val arrow: ImageView = view.findViewById(R.id.iv_arrow)          // Flecha para expandir/colapsar
        val layoutProducts: LinearLayout = view.findViewById(R.id.layout_products) // Contenedor de productos
    }

    // Inflamos el layout XML de cada ítem de lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ListViewHolder(view)
    }

    // Asociamos los datos de cada ShoppingList con las vistas del ViewHolder
    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val item = list[position]

        // Mostramos la fecha y el supermercado
        holder.dateTime.text = item.dateTime
        holder.store.text = item.storeName

        // Calculamos el total sumando precios * cantidades, ignorando productos no disponibles
        val total = item.products
            .filter { it.unitPrice != -1.0 }
            .sumOf { it.unitPrice * it.quantity }

        // Mostramos el total formateado en euros
        holder.itemView.findViewById<TextView>(R.id.tv_total).text =
            "Total: %.2f€".format(total)

        // Antes de añadir nuevos productos, limpiamos el layout (evita duplicados al reciclar vistas)
        holder.layoutProducts.removeAllViews()

        if (item.isExpanded) {
            // Si el ítem está expandido, mostramos el contenedor de productos
            holder.layoutProducts.visibility = View.VISIBLE
            holder.arrow.rotation = 180f // Rotamos la flecha hacia arriba

            // Para cada producto de la lista, creamos dinámicamente un TextView
            item.products.forEach { product ->
                val tv = TextView(holder.itemView.context).apply {
                    text = if (product.unitPrice == -1.0) {
                        // Si no disponible, mostramos un texto especial
                        "${product.name} - x${product.quantity} - No disponible"
                    } else {
                        // Si disponible, mostramos el nombre, cantidad y precio total
                        "${product.name} - x${product.quantity} - ${"%.2f".format(product.quantity * product.unitPrice)}€"
                    }
                    setTextColor(ContextCompat.getColor(context, R.color.smoky_black)) // Color del texto
                    setPadding(0, 4, 0, 4) // Espaciado entre productos
                }
                holder.layoutProducts.addView(tv) // Añadimos el producto al layout
            }

        } else {
            // Si el ítem no está expandido, ocultamos el contenedor de productos
            holder.layoutProducts.visibility = View.GONE
            holder.arrow.rotation = 0f // Flecha hacia abajo
        }

        // Configuramos el click para expandir/colapsar la vista al tocar el ítem completo
        holder.itemView.setOnClickListener {
            item.isExpanded = !item.isExpanded // Cambiamos el estado expandido
            notifyItemChanged(position) // Refrescamos solo este ítem
        }
    }

    // Devuelve el número de elementos que maneja el adaptador
    override fun getItemCount(): Int = list.size
}
