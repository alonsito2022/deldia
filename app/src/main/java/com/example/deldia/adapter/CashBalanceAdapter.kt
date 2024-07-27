package com.example.deldia.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.models.Cash
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class CashBalanceAdapter(val dataSet: ArrayList<Cash>, private val mListener: OnItemClickListener?) : RecyclerView.Adapter<CashBalanceAdapter.ViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(model: Cash)
    }
    private var context: Context? = null
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val textViewCashBalance: TextView
        val textViewCashName: TextView
        val view: View
        init {

            textViewCashBalance = itemView.findViewById(R.id.textViewCashBalance)
            textViewCashName = itemView.findViewById(R.id.textViewCashName)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashBalanceAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cash_cardview, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: CashBalanceAdapter.ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.textViewCashName.text = item.name.uppercase()
        holder.textViewCashBalance.text = "S/ ${item.balance}"

        holder.view.setOnClickListener {
            mListener!!.onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}