package com.sys4soft.deldia.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.models.Operation.OperationDetail

class OperationDetailAdapter(val dataSet: MutableList<OperationDetail>, private val mListener: OnItemClickListener?) : RecyclerView.Adapter<OperationDetailAdapter.ViewHolder>()  {
    interface OnItemClickListener {
        fun onItemClick(model: OperationDetail, position:Int)
    }
    private var context: Context? = null
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val switchEnlisted: Switch
        val textViewProductName: TextView
        val textViewQuantity: TextView
        val view: View
        init {

            switchEnlisted = itemView.findViewById(R.id.switchEnlisted)
            textViewProductName = itemView.findViewById(R.id.textViewProductName)
            textViewQuantity = itemView.findViewById(R.id.textViewQuantity)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_operation_detail_two_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.textViewProductName.text = item.productSaleName
        holder.textViewQuantity.text = item.quantity.toString()

        holder.switchEnlisted.isChecked = item.enlisted

        holder.view.setOnClickListener {
            mListener!!.onItemClick(item, position)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}