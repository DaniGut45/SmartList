package com.example.smartlist.view.adapters

import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlist.R
import com.example.smartlist.model.ShoppingList

class ListAdapter(
    private val list: MutableList<ShoppingList>,
    private val onDeleteClick: (ShoppingList) -> Unit // Callback para eliminar
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    inner class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTime: TextView = view.findViewById(R.id.tv_date_time)
        val store: TextView = view.findViewById(R.id.tv_store)
        val arrow: ImageView = view.findViewById(R.id.iv_arrow)
        val layoutProducts: LinearLayout = view.findViewById(R.id.layout_products)
        val total: TextView = view.findViewById(R.id.tv_total)
        val deleteButton: ImageButton = view.findViewById(R.id.btn_delete)
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

        val total = item.products
            .filter { it.unitPrice != -1.0 }
            .sumOf { it.unitPrice * it.quantity }

        holder.total.text = "Total: %.2f€".format(total)

        holder.layoutProducts.removeAllViews()

        if (item.isExpanded) {
            holder.layoutProducts.visibility = View.VISIBLE
            holder.arrow.rotation = 180f

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
            holder.arrow.rotation = 0f
        }

        holder.itemView.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position)
        }

        // Callback de eliminar
        holder.deleteButton.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = list.size
}
