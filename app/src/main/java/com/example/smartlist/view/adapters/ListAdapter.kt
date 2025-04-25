package com.example.smartlist.view.adapters

import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlist.R
import com.example.smartlist.model.ShoppingList

class ListAdapter(
    private val list: MutableList<ShoppingList>
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    // ViewHolder que contiene las vistas necesarias para mostrar cada lista de compras.
    // Solo se inicializan las vistas base (fecha, tienda, flecha, y contenedor para productos).
    inner class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTime: TextView = view.findViewById(R.id.tv_date_time)
        val store: TextView = view.findViewById(R.id.tv_store)
        val arrow: ImageView = view.findViewById(R.id.iv_arrow)
        val layoutProducts: LinearLayout = view.findViewById(R.id.layout_products)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val item = list[position]
        holder.dateTime.text = item.dateTime
        holder.store.text = item.storeName

        // Se calcula el total de la compra en tiempo real sumando cantidad * precio unitario de cada producto.
        val total = item.products
            .filter { it.unitPrice != -1.0 }
            .sumOf { it.unitPrice * it.quantity }
        holder.itemView.findViewById<TextView>(R.id.tv_total).text =
            "Total: %.2f€".format(total)

        // Se limpia el layout antes de volver a inflar las vistas de productos (prevención de duplicados).
        holder.layoutProducts.removeAllViews()

        if (item.isExpanded) {
            holder.layoutProducts.visibility = View.VISIBLE
            holder.arrow.rotation = 180f // Flecha rotada hacia arriba

            // Por cada producto en la lista, se crea y configura dinámicamente un TextView.
            item.products.forEach { product ->
                val tv = TextView(holder.itemView.context).apply {
                    text = if (product.unitPrice == -1.0) {
                        "${product.name} - x${product.quantity} - No disponible"
                    } else {
                        "${product.name} - x${product.quantity} - ${"%.2f".format(product.quantity * product.unitPrice)}€"
                    }
                    setTextColor(ContextCompat.getColor(context, R.color.smoky_black))
                    setPadding(0, 4, 0, 4)
                }
                holder.layoutProducts.addView(tv)
            }

        } else {
            holder.layoutProducts.visibility = View.GONE
            holder.arrow.rotation = 0f // Flecha hacia abajo
        }

        // Maneja la expansión/colapso de la vista al hacer clic sobre el ítem completo.
        holder.itemView.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position) // Se actualiza solo el ítem actual
        }
    }

    override fun getItemCount(): Int = list.size
}
