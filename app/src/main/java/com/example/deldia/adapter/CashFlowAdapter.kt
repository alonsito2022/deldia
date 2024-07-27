package com.example.deldia.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.models.CashFlow

class CashFlowAdapter(var dataSet: ArrayList<CashFlow>): RecyclerView.Adapter<CashFlowAdapter.ViewHolder>()  {

    private var context: Context? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val cardDescription: TextView
        val cardType: TextView
        val cardDate: TextView
        val cardClientTotal: TextView
        val cardCashName: TextView

        val view: View
        init {
            cardDescription = itemView.findViewById(R.id.cardDescription)
            cardType = itemView.findViewById(R.id.cardType)
            cardDate = itemView.findViewById(R.id.cardDate)
            cardClientTotal = itemView.findViewById(R.id.cardClientTotal)
            cardCashName = itemView.findViewById(R.id.cardCashName)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashFlowAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cash_flow_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: CashFlowAdapter.ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.cardDescription.text = item.description
        holder.cardType.text = item.typeDisplay
        holder.cardDate.text = item.transactionDate
        holder.cardClientTotal.text = "S/ ${item.total}"
        holder.cardCashName.text = item.cashName

    }

    override fun getItemCount(): Int {
        return dataSet.size
    }


}