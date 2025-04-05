package com.example.smartlist.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlist.R
import com.example.smartlist.model.ShoppingList

class ListAdapter(private val list: List<ShoppingList>) :
    RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTime: TextView = view.findViewById(R.id.tv_date_time)
        val store: TextView = view.findViewById(R.id.tv_store)
        val arrow: ImageView = view.findViewById(R.id.iv_arrow)
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
    }

    override fun getItemCount(): Int = list.size
}
