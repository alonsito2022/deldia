package com.example.deldia.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.models.Operation

class OperationDetailUnsavedAdapter(val dataSet: MutableList<Operation.OperationDetail>): RecyclerView.Adapter<OperationDetailUnsavedAdapter.ViewHolder>(){
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val textViewPickingDetailProductName: TextView
        val textViewPickingDetailPrice: TextView
        val textViewPickingDetailQuantity: TextView
        val textViewPickingDetailSubtotal: TextView
        val view: View

        init {
            textViewPickingDetailProductName = itemView.findViewById(R.id.textViewPickingDetailProductName)
            textViewPickingDetailPrice = itemView.findViewById(R.id.textViewPickingDetailPrice)
            textViewPickingDetailQuantity = itemView.findViewById(R.id.textViewPickingDetailQuantity)
            textViewPickingDetailSubtotal = itemView.findViewById(R.id.textViewPickingDetailSubtotal)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_firebase_picking_detail_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.textViewPickingDetailProductName.text = item.productSaleName
        holder.textViewPickingDetailPrice.text = item.price.toString()
        holder.textViewPickingDetailQuantity.text = item.quantity.toString()
        val totalF3:Double = Math.round((item.price * item.quantity) * 1000.0) / 1000.0
        val totalF1:Double = Math.round(totalF3 * 100.0) / 100.0
        holder.textViewPickingDetailSubtotal.text = totalF1.toString()
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}